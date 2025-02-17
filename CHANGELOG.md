# CodeScene Intellij Plugin Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
## [0.1.2-codescene-eap] - 2025-02-17
### Fixed
- Documentation tab button focuses on the correct file when two files in a project have the same names.

<!-- Placeholder for 0.1.1-codescene-eap -->

## [0.1.0-codescene-eap] - 2025-02-07
### Added
- Progress indicators for review and monitor.

## [0.0.14-beta] - 2025-02-07
### Changed
- Code Health text in details panel
- Code delta 0.0 score will show "N/A" instead

## [0.0.13-beta] - 2025-02-04
### Fixed
- Removed Deprecated & Internal API usage

## [0.0.12-beta] - 2025-01-31
### Changed
- Plugin description in plugin.xml

## [0.0.11-beta] - 2025-01-31
### Added
- Telemetry consent prompt and state (false by default).

### Changed
- Telemetry events will only be logged once user consent is given.

## [0.0.10-beta] - 2025-01-31
### Changed
- Telemetry refactored and improved

## [0.0.9-beta] - 2025-01-29
### Added
- CodeScene telemetry to track user activity

## [0.0.8-beta] - 2025-01-27
### Changed
- Migrated from *DevToolsAPI* library to *ExtensionAPI* library.

### Removed
- Deserialization logic, as the new library returns POJOs directly.

## [0.0.7-beta] - 2025-01-23
### Added
- Help action for the Code Health Monitor added to the tool window, enabling users to access its documentation

### Changed
- Clicking on a Code Health code vision shows the tool window if it was not in focus when a monitor entry exists; otherwise, it opens the documentation file about Code Health. 
- Adjusted documentation parsing and opening logic to support standalone documentation (Code Health & Code Health Monitor).

## [0.0.6-beta] - 2025-01-21
### Fixed
- Resolved an issue in the Code Health Monitor where multiple issues within the same method but at different lines displayed the same details in the Code Health Details section.

## [0.0.5-beta] - 2025-01-20
### Fixed
- Resolved an issue where the Code Health Monitor displayed inconsistent or mixed results when multiple IDE instances were open. Each instance now displays accurate, independent results for its project.

## [0.0.4-beta] - 2025-01-16
### Added
- About tab contents in Tools > CodeScene > About
- General tab contents in Tools > CodeScene > General

### Changed
- Removed unused/unimplemented settings from Tools > CodeScene > Settings

## [0.0.3-beta] - 2025-01-10
### Added
- Code health details view (health, file, function-level) in CodeScene tool window

### Changed
- Code health documentation access strategy from in-repo to pulling from a common repository
- Small UI/UX improvements

## [0.0.2-beta] - 2024-12-25
### Added
- Code health documentation
- Custom markdown previewer

## [0.0.1-beta] - 2024-12-13
### Added
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

