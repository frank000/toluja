package main

import (
	"bytes"
	"context"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
	"sync"
	"time"
)

type config struct {
	APIBaseURL     string
	DeviceID       string
	PrintKey       string
	PollIntervalMs int
}

type nextJobResponse struct {
	JobID         string        `json:"jobId"`
	TenantID      string        `json:"tenantId"`
	StoreID       string        `json:"storeId"`
	DeviceID      string        `json:"deviceId"`
	OrderID       string        `json:"orderId"`
	PayloadType   string        `json:"payloadType"`
	PayloadBase64 string        `json:"payloadBase64"`
	CreatedAt     string        `json:"createdAt"`
	Deliveries    []jobDelivery `json:"deliveries"`
}

type jobDelivery struct {
	DeliveryID  string `json:"deliveryId"`
	PrinterID   string `json:"printerId"`
	PrinterName string `json:"printerName"`
	Channel     string `json:"channel"`
	Destination string `json:"destination"`
	Copies      int    `json:"copies"`
}

type ackRequest struct {
	Deliveries []deliveryAck `json:"deliveries"`
}

type deliveryAck struct {
	DeliveryID   string    `json:"deliveryId"`
	Status       string    `json:"status"`
	ErrorMessage *string   `json:"errorMessage"`
	PrintedAt    time.Time `json:"printedAt"`
}

var (
	version   = "dev"
	commit    = "none"
	buildTime = "unknown"
)

func main() {
	loadDotEnvIfPresent()

	cfg, err := loadConfig()
	if err != nil {
		panic(err)
	}

	client := &http.Client{Timeout: 20 * time.Second}
	runningAsService, err := runAsWindowsService(func(stop <-chan struct{}) error {
		return runLoop(client, cfg, stop)
	})
	if err != nil {
		panic(err)
	}
	if runningAsService {
		return
	}

	stop := make(chan struct{})
	if err := runLoop(client, cfg, stop); err != nil {
		panic(err)
	}
}

func runLoop(client *http.Client, cfg config, stop <-chan struct{}) error {
	ticker := time.NewTicker(time.Duration(cfg.PollIntervalMs) * time.Millisecond)
	defer ticker.Stop()

	fmt.Printf(
		"agent started device=%s api=%s version=%s commit=%s buildTime=%s\n",
		cfg.DeviceID,
		cfg.APIBaseURL,
		version,
		commit,
		buildTime,
	)

	for {
		if err := runOnce(client, cfg); err != nil {
			fmt.Printf("loop error: %v\n", err)
		}

		select {
		case <-stop:
			return nil
		case <-ticker.C:
		}
	}
}

func runOnce(client *http.Client, cfg config) error {
	job, err := fetchNextJob(client, cfg)
	if err != nil {
		return err
	}
	if job == nil {
		return nil
	}

	payload, err := base64.StdEncoding.DecodeString(job.PayloadBase64)
	if err != nil {
		return fmt.Errorf("invalid payloadBase64: %w", err)
	}

	results := make([]deliveryAck, len(job.Deliveries))
	var wg sync.WaitGroup
	for i, delivery := range job.Deliveries {
		wg.Add(1)
		go func(idx int, d jobDelivery) {
			defer wg.Done()
			results[idx] = printDelivery(d, payload)
		}(i, delivery)
	}
	wg.Wait()

	return sendAck(client, cfg, job.JobID, ackRequest{
		Deliveries: results,
	})
}

func fetchNextJob(client *http.Client, cfg config) (*nextJobResponse, error) {
	req, err := http.NewRequest(http.MethodGet, strings.TrimRight(cfg.APIBaseURL, "/")+"/api/print-agent/jobs/next?deviceId="+cfg.DeviceID, nil)
	if err != nil {
		return nil, err
	}
	req.Header.Set("X-Print-Key", cfg.PrintKey)

	res, err := client.Do(req)
	if err != nil {
		return nil, err
	}
	defer res.Body.Close()

	if res.StatusCode == http.StatusNoContent {
		return nil, nil
	}
	if res.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(res.Body)
		return nil, fmt.Errorf("next job failed status=%d body=%s", res.StatusCode, string(body))
	}

	var job nextJobResponse
	if err := json.NewDecoder(res.Body).Decode(&job); err != nil {
		return nil, err
	}
	return &job, nil
}

func printDelivery(d jobDelivery, payload []byte) deliveryAck {
	now := time.Now().UTC()
	err := dispatchPrint(d, payload)
	if err != nil {
		msg := err.Error()
		return deliveryAck{
			DeliveryID:   d.DeliveryID,
			Status:       "ERROR",
			ErrorMessage: &msg,
			PrintedAt:    now,
		}
	}

	return deliveryAck{
		DeliveryID: d.DeliveryID,
		Status:     "SUCCESS",
		PrintedAt:  now,
	}
}

func dispatchPrint(d jobDelivery, payload []byte) error {
	if d.Copies < 1 {
		return errors.New("copies must be >= 1")
	}

	channel := strings.ToUpper(strings.TrimSpace(d.Channel))
	switch channel {
	case "CUPS":
		return printCUPS(d.Destination, d.Copies, payload)
	case "WINDOWS_QUEUE":
		return printWindowsQueue(d.Destination, d.Copies, payload)
	default:
		return fmt.Errorf("unsupported channel: %s", d.Channel)
	}
}

func printCUPS(queue string, copies int, payload []byte) error {
	if runtime.GOOS == "windows" {
		return errors.New("CUPS is not supported on Windows")
	}
	cmd := exec.Command("lp", "-d", queue, "-n", fmt.Sprintf("%d", copies))
	cmd.Stdin = bytes.NewReader(payload)
	output, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("cups print failed: %w output=%s", err, string(output))
	}
	return nil
}

func printWindowsQueue(queue string, copies int, payload []byte) error {
	if runtime.GOOS != "windows" {
		return errors.New("WINDOWS_QUEUE is supported only on Windows")
	}

	tmpFile, err := os.CreateTemp("", "print-agent-*.txt")
	if err != nil {
		return err
	}
	defer func() {
		_ = os.Remove(tmpFile.Name())
	}()
	if _, err := tmpFile.Write(payload); err != nil {
		_ = tmpFile.Close()
		return err
	}
	_ = tmpFile.Close()

	for i := 0; i < copies; i++ {
		command := fmt.Sprintf("Get-Content -Raw '%s' | Out-Printer -Name '%s'", tmpFile.Name(), queue)
		cmd := exec.Command("powershell", "-NoProfile", "-Command", command)
		output, err := cmd.CombinedOutput()
		if err != nil {
			return fmt.Errorf("windows print failed: %w output=%s", err, string(output))
		}
	}
	return nil
}

func sendAck(client *http.Client, cfg config, jobID string, ack ackRequest) error {
	body, err := json.Marshal(ack)
	if err != nil {
		return err
	}

	ctx, cancel := context.WithTimeout(context.Background(), 20*time.Second)
	defer cancel()

	url := strings.TrimRight(cfg.APIBaseURL, "/") + "/api/print-agent/jobs/" + jobID + "/ack"
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, url, bytes.NewReader(body))
	if err != nil {
		return err
	}
	req.Header.Set("X-Print-Key", cfg.PrintKey)
	req.Header.Set("Content-Type", "application/json")

	res, err := client.Do(req)
	if err != nil {
		return err
	}
	defer res.Body.Close()
	if res.StatusCode != http.StatusOK {
		respBody, _ := io.ReadAll(res.Body)
		return fmt.Errorf("ack failed status=%d body=%s", res.StatusCode, string(respBody))
	}
	return nil
}

func loadConfig() (config, error) {
	poll := 1000
	if v := strings.TrimSpace(os.Getenv("POLL_INTERVAL_MS")); v != "" {
		_, err := fmt.Sscanf(v, "%d", &poll)
		if err != nil {
			return config{}, fmt.Errorf("invalid POLL_INTERVAL_MS: %w", err)
		}
	}

	cfg := config{
		APIBaseURL:     strings.TrimSpace(os.Getenv("API_BASE_URL")),
		DeviceID:       strings.TrimSpace(os.Getenv("DEVICE_ID")),
		PrintKey:       strings.TrimSpace(os.Getenv("PRINT_KEY")),
		PollIntervalMs: poll,
	}
	if cfg.APIBaseURL == "" || cfg.DeviceID == "" || cfg.PrintKey == "" {
		return config{}, errors.New("required env vars: API_BASE_URL, DEVICE_ID, PRINT_KEY")
	}
	return cfg, nil
}

func loadDotEnvIfPresent() {
	path := ".env"
	if _, err := os.Stat(path); err != nil {
		return
	}

	content, err := os.ReadFile(filepath.Clean(path))
	if err != nil {
		return
	}

	lines := strings.Split(string(content), "\n")
	for _, rawLine := range lines {
		line := strings.TrimSpace(rawLine)
		if line == "" || strings.HasPrefix(line, "#") {
			continue
		}
		parts := strings.SplitN(line, "=", 2)
		if len(parts) != 2 {
			continue
		}
		key := strings.TrimSpace(parts[0])
		val := strings.TrimSpace(parts[1])
		if key == "" {
			continue
		}
		if _, exists := os.LookupEnv(key); exists {
			continue
		}
		_ = os.Setenv(key, val)
	}
}
