#!/usr/bin/env bash
pkill -f "idea-sandbox.*codescene-jetbrains" 2>/dev/null || true
pkill -f "GradleWrapperMain.*runIde" 2>/dev/null || true
pkill -f "make.*run-ide" 2>/dev/null || true
