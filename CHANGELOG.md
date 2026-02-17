# CodeScene Intellij Plugin Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

- **Changed**
  - Default timeout increased from 15s to 60s.
- **Fixed**
  - Handling of empty old/new scores, mainly affecting the old native monitor presentation.
  - Review Code Vision now automatically refreshes for all open files across all projects when settings change, without requiring manual file content refresh.

### [0.4.2] - 2026-02-17
- **Fixed**
  - Fixed API Compatibility problems with IntelliJ 2026.1

### [0.4.1] - 2025-11-03
- **Fixed**
  - Bump Extension API version 1.0-2935bd0
  - Ensure finding PSI file happens on the BG thread under read action
- **Changed**
  - Increase analysis timeout to 60s (previously 15s)
- **Added**
  - Telemetry event for analysis performance

### [0.4.0] - 2025-10-14
- **Changed**
  - Add the Code Health Monitor to Freemium
  - Bump Extension API version 1.0-10c2a47

### [0.3.2] - 2025-09-23
- **Changed**
    - Update ACE wording

### [0.3.1] - 2025-09-16
- **Changed**
    - Bump Extension API version 1.0.8
    - Improve code health review time by 50%

### [0.3.0] - 2025-08-14
- **Fixed**
    - Added user-friendly error messages
    - Device id generation on Windows OS
- **Changed**
    - Removed ACE from public version
    - Bump Extension API version 1.0.5

### [0.2.2] - 2025-08-06
- **Changed**
    - Bump Extension API version
- **Fixed**
    - Bugs related to IntelliJ's threading model

### [0.2.1] - 2025-04-29
- **Changed**
    - Enable ACE refactoring by default
- **Fixed**
    - Minor ACE fixes and improvements

### [0.2.0] - 2025-04-28
- **Added**
    - ACE entry points from:
        - Code Health details,
        - code vision (lens),
        - intention actions (in-editor + problems tab)
    - ACE promotion and policy box as the first step upon using the entry points.
    - ACE refactoring results UI to show refactoring info
    - Buttons in refactoring results UI
        - Accept Auto-Refactor - to apply proposed refactoring
        - Reject - to reject proposed refactoring
        - Retry Auto-Refactor - to retry refactoring

### [0.1.1] - 2025-04-11
- **Added**
  - Device ID:
    - to track users with enabled analytics,
    - presented in About tab.

### [0.1.0] - 2025-02-27
- **Added**
  - Plugin settings UI form allowing users to configure options such as enabling code vision and choosing whether to analyze files ignored by .gitignore.
  - Code vision providers that display review findings directly within the editor for better visibility of detected issues.
  - External annotator that highlights review findings inline within the code, providing immediate feedback.
  - Code health documentation access, allowing users to click on code vision markers to view detailed information about detected code smells or open standalone documentation.
  - Help action for the Code Health Monitor (beta) in the tool window, giving users quick access to relevant documentation.
  - CodeScene telemetry system to track user activity, activated only after explicit user consent.
  - Telemetry consent prompt that asks users for permission before collecting any data, with consent disabled by default.
  - Progress indicator for the review process
