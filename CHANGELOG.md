# CodeScene Intellij Plugin Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
- **Fixed**
    - Added user-friendly error messages

### [0.2.3-codescene-eap] - 2025-08-05
- **Changed**
  - Bump Extension API version
- **Fixed**
  - Bugs related to IntelliJ's threading model
  
### [0.2.2-codescene-eap] - 2025-07-01
- **Fixed**
  - Minor ACE fixes and improvements
  - Unhandled exception on empty range optional

### [0.2.1-codescene-eap] - 2025-04-29
- **Changed**
  - Enable ACE refactoring by default
- **Fixed**
  - Minor ACE fixes and improvements

### [0.2.0-codescene-eap] - 2025-04-25
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

### [0.1.5-codescene-eap] - 2025-04-11
- **Added**
  - Device ID:
    - to track users with enabled analytics,
    - presented in About tab.

### [0.1.4-codescene-eap] - 2025-03-17
- **Changed**
  - Code Health Monitor:
    - baseline from HEAD commit to branch creation commit,
    - documentation.

### [0.1.3-codescene-eap] - 2025-02-21
- **Changed**
  - Hidden CodeScene server URL in settings

### [0.1.2-codescene-eap] - 2025-02-17
- **Fixed**
  - Documentation tab button focuses on the correct file when two files in a project have the same names.

### [0.1.1-codescene-eap] - 2025-02-17
- **Added**
  - Code Health Monitor tracks improvements, informing the user of previously committed code smells that have now been fixed.
  - Three sorting options to Code Health Monitor tree:
    - ascending (sort by largest decrease first),
    - descending (sort by smallest decrease first),
    - file name (sort by file name - ascending).
  - "Collapse all" option to Code Health Monitor.
- **Changed**
  - Code Health Monitor
    - collapsed nodes show number of improvable functions and change percentage (if present),
    - tooltips and function findings' icons reflect improvement state.
  - Code Health details:
    - improvement opportunities and fixed code smells outlined in the details tab of findings.
- **Fixed**
  - Delta analysis will not be re-triggered if the API response for an unchanged file is `null`.

### [0.1.0-codescene-eap] - 2025-02-07
- **Added**
  - Progress indicators for review and monitor.

### [0.0.14-beta] - 2025-02-07
- **Changed**
  - Code Health text in details panel
  - Code delta 0.0 score will show "N/A" instead

### [0.0.13-beta] - 2025-02-04
- **Fixed**
  - Removed Deprecated & Internal API usage

### [0.0.12-beta] - 2025-01-31
- **Changed**
  - Plugin description in plugin.xml

### [0.0.11-beta] - 2025-01-31
- **Added**
  - Telemetry consent prompt and state (false by default).
- **Changed**
  - Telemetry events will only be logged once user consent is given.

### [0.0.10-beta] - 2025-01-31
- **Changed**
  - Telemetry refactored and improved

### [0.0.9-beta] - 2025-01-29
- **Added**
  - CodeScene telemetry to track user activity

### [0.0.8-beta] - 2025-01-27
- **Changed**
  - Migrated from *DevToolsAPI* library to *ExtensionAPI* library.
- **Removed**
  - Deserialization logic, as the new library returns POJOs directly.

### [0.0.7-beta] - 2025-01-23
- **Added**
  - Help action for the Code Health Monitor added to the tool window, enabling users to access its documentation
- **Changed**
  - Clicking on a Code Health code vision shows the tool window if it was not in focus when a monitor entry exists; otherwise, it opens the documentation file about Code Health. 
  - Adjusted documentation parsing and opening logic to support standalone documentation (Code Health & Code Health Monitor).

### [0.0.6-beta] - 2025-01-21
- **Fixed**
  - Resolved an issue in the Code Health Monitor where multiple issues within the same method but at different lines displayed the same details in the Code Health Details section.

### [0.0.5-beta] - 2025-01-20
- **Fixed**
  - Resolved an issue where the Code Health Monitor displayed inconsistent or mixed results when multiple IDE instances were open. Each instance now displays accurate, independent results for its project.

### [0.0.4-beta] - 2025-01-16
- **Added**
  - About tab contents in Tools > CodeScene > About
  - General tab contents in Tools > CodeScene > General
- **Changed**
  - Removed unused/unimplemented settings from Tools > CodeScene > Settings

### [0.0.3-beta] - 2025-01-10
- **Added**
  - Code health details view (health, file, function-level) in CodeScene tool window
- **Changed**
  - Code health documentation access strategy from in-repo to pulling from a common repository
  - Small UI/UX improvements

### [0.0.2-beta] - 2024-12-25
- **Added**
  - Code health documentation
  - Custom markdown previewer

### [0.0.1-beta] - 2024-12-13
- **Added**
  - Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
  - Plugin settings UI form for user configuration, including logic for saving settings
  - Tool window placeholder for future feature expansion
  - Code health review
  - Code vision providers to display review findings within the editor
  - External annotator to highlight review findings directly in the code
  - Placeholder for intention actions to address review findings
  - Icons for code health
  - Code health monitoring functionality (delta)
  - Code health monitoring tool window