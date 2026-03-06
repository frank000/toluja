//go:build windows

package main

import (
	"errors"
	"fmt"
	"time"

	"golang.org/x/sys/windows/svc"
)

type serviceRunner struct {
	run func(stop <-chan struct{}) error
}

func (r *serviceRunner) Execute(_ []string, requests <-chan svc.ChangeRequest, status chan<- svc.Status) (bool, uint32) {
	const accepted = svc.AcceptStop | svc.AcceptShutdown
	status <- svc.Status{State: svc.StartPending}

	stop := make(chan struct{})
	runErr := make(chan error, 1)
	go func() {
		runErr <- r.run(stop)
	}()

	status <- svc.Status{State: svc.Running, Accepts: accepted}

	for {
		select {
		case req := <-requests:
			switch req.Cmd {
			case svc.Interrogate:
				status <- req.CurrentStatus
			case svc.Stop, svc.Shutdown:
				status <- svc.Status{State: svc.StopPending}
				close(stop)
				select {
				case err := <-runErr:
					if err != nil {
						fmt.Printf("service loop error: %v\n", err)
					}
				case <-time.After(15 * time.Second):
				}
				return false, 0
			}
		case err := <-runErr:
			if err != nil {
				fmt.Printf("service loop error: %v\n", err)
				return false, 1
			}
			return false, 0
		}
	}
}

func runAsWindowsService(run func(stop <-chan struct{}) error) (bool, error) {
	isWindowsService, err := svc.IsWindowsService()
	if err != nil {
		return false, err
	}
	if !isWindowsService {
		return false, nil
	}
	if run == nil {
		return true, errors.New("service runner is nil")
	}

	if err := svc.Run("TolujaPrintAgent", &serviceRunner{run: run}); err != nil {
		return true, err
	}
	return true, nil
}
