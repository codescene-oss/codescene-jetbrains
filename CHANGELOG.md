# CodeScene Intellij Plugin Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
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

