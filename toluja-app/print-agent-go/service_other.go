//go:build !windows

package main

func runAsWindowsService(_ func(stop <-chan struct{}) error) (bool, error) {
	return false, nil
}
