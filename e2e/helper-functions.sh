#!/bin/bash

function create-fake-notifications-service() {
    oc run fake-notifications-service --image=docker.io/golang:1.21 --port=8080 -- bash -c 'cat <<EOF > main.go && go run main.go
package main

import (
    "fmt"
    "log"
    "net/http"
)
func main() {
    log.Fatal(http.ListenAndServe(":8080", http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) { fmt.Println(r) })))
}
EOF
'
    kubectl wait --for=condition=Ready=true pods fake-notifications-service --timeout=5m
    kubectl expose pods/fake-notifications-service
}
