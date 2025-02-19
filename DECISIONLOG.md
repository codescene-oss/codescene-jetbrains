# CodeScene Plugin Decision Log

The purpose of this Decision Log is to document important decisions made during the development of the CodeScene 
plugin. It helps provide clarity and rationale for each decision, making it easier to understand why certain
choices were made and to review them later if needed. This log serves as a reference for current and future team
members, ensuring transparency and consistency throughout the project lifecycle.

---

## Table of Contents

1. [DEC-2024-09-12-001: Choose Kotlin for Plugin Development](#dec-2024-09-12-001-choose-kotlin-for-plugin-development)
2. [DEC-2024-09-12-002: Determine Supported Product Versions for the Plugin](#dec-2024-09-12-002-determine-supported-product-versions-for-the-plugin)
3. [DEC-2024-09-12-003: Choose JDK 17 for Plugin Development](#dec-2024-09-12-003-choose-jdk-17-for-plugin-development)
4. [DEC-2024-09-18-001: Update Supported Product Versions for the Plugin](#dec-2024-09-18-001-update-supported-product-versions-for-the-plugin)
5. [DEC-2024-10-04-001: Multiple CodeVision Providers for Distinct Code Smells](#dec-2024-10-04-001-multiple-codevision-providers-for-distinct-code-smells)
6. [DEC-2025-01-03-001: Code Smell Documentation Location Update](#dec-2025-01-03-001-code-smell-documentation-location-update)

---

## DEC-2024-09-12-001: Choose Kotlin for Plugin Development

[Back to Top](#table-of-contents)

**Date**: 2024-09-12

**Decision Maker(s)**: Dzenan Granulo, Selma Copra

**Status**: Accepted

**Decision**:
The project will use Kotlin for developing the plugin.

**Context**:
We needed a language that is fully supported by JetBrains IDEs and provides modern features for plugin development.
Kotlin is known for its concise syntax and compatibility with existing Java code.

**Rationale**:
Kotlin was chosen because it is officially supported by JetBrains and provides better type safety and null-safety
features compared to Java. Although the Platform API was written in Java, Kotlin is fully interoperable with the API and
is preferred for its modern features. Many Platform features require Kotlin to function effectively, and understanding
Java is only necessary when reviewing the documentation or the API code.

**Consequences**:
This decision means the team will need to be proficient in Kotlin. It also aligns the project with modern practices and
JetBrains’ recommended approach.

**Alternatives Considered**:

- **Java**: While Java is a well-known option, it lacks some of the modern features that Kotlin provides, like
  coroutines.

**Related Decisions**:
None

---

## DEC-2024-09-12-002: Determine Supported Product Versions for the Plugin

[Back to Top](#table-of-contents)

> **_NOTE:_**  Decision overruled
> by: [DEC-2024-09-18-001: Update Supported Product Versions for the Plugin](#dec-2024-09-18-001-update-supported-product-versions-for-the-plugin).

**Date**: 2024-09-12

**Decision Maker(s)**: Dzenan Granulo, Selma Copra

**Status**: Overruled

**Decision**:
The plugin will support IntelliJ IDEA versions from the last four major releases: 2024.X, 2023.X, 2022.X, 2021.3 and
2021.2.

**Context**:
To provide a balanced support coverage while avoiding the need to release updates for every version of the JetBrains
IDE, it is essential to focus on the versions that are most widely used by the user base. Supporting a broader range of
versions can increase the plugin's accessibility but also requires more maintenance.

**Rationale**:
Statistics from August 2023 show that the majority of users are on the latest major release and the two previous major
releases. Supporting these versions will cover approximately 80% of the user base, addressing the needs of the vast
majority of users while maintaining a manageable maintenance workload. The statistics indicate:

- The latest version is used by roughly 50% of users.
- Supporting the latest major release and the two preceding releases will ensure broad compatibility without excessive
  support burden.

**Consequences**:
The plugin needs to be tested and maintained for compatibility with IntelliJ IDEA 2021.3, 2021.2, 2022.X, 2023.X, and
2024.X.

**Alternatives Considered**:

- **Supporting All Versions**: This approach would be more comprehensive but would increase maintenance efforts
  significantly.
- **Supporting Only the Latest Version**: This would reduce maintenance but might exclude a significant portion of
  users.

**Related Decisions**:
None

**Additional Resources**:

- [JetBrains Product Versions in Use Statistics](https://plugins.jetbrains.com/docs/marketplace/product-versions-in-use-statistics.html) [date of access: September 2024]

## DEC-2024-09-12-003: Choose JDK 17 for Plugin Development

[Back to Top](#table-of-contents)

**Date**: 2024-09-12

**Decision Maker(s)**: Dzenan Granulo, Selma Copra

**Status**: Accepted

**Decision**:
The project will use JDK 17 for developing the plugin. The plugin will support IntelliJ IDEA versions from
2021.X to 2024.X.

**Context**:
We needed to select a JDK version that aligns with the JetBrains IDE versions that the plugin will support, ensuring
compatibility and future-proofing.

**Rationale**:
JDK 17 was chosen because it supports the range of JetBrains IDE versions from IntelliJ IDEA 2021.X to 2024.X. This
covers all the major releases we plan to support:

- **IntelliJ IDEA 2024.X**: Up to Java 22
- **IntelliJ IDEA 2023.X**: Up to Java 20
- **IntelliJ IDEA 2022.X**: Up to Java 18
- **IntelliJ IDEA 2021.X**: Up to Java 17 (excluding *IntelliJ IDEA 2021.1*)

JDK 17 is the lowest supported version among all these IDE versions and ensures compatibility with key features like
sealed types and always-strict floating-point semantics.

**Consequences**:
This decision means the project will use JDK 17 for development, which aligns with the needs of a wide user base and
future-proofs the plugin against upcoming IDE versions.

**Alternatives Considered**:
None

**Related Decisions**:

- [DEC-2024-09-12-002: Determine Supported Product Versions for the Plugin](#dec-2024-09-12-002-determine-supported-product-versions-for-the-plugin)

**Additional Resources**:

- [Supported Java versions and features](https://www.jetbrains.com/help/idea/supported-java-versions.html) [date of access: September 2024]

## DEC-2024-09-18-001: Update Supported Product Versions for the Plugin

[Back to Top](#table-of-contents)

> **_NOTE:_**  Decision overruled
> by: [## DEC-2024-12-23-001: Update Supported Product Versions for the Plugin](#dec-2024-12-23-001-update-supported-product-versions-for-the-plugin).

**Date**: 2024-09-18

**Decision Maker(s)**: Dzenan Granulo, Selma Copra

**Status**: Accepted

**Decision**:
The plugin will support IntelliJ IDEA versions from the last two major releases: 2024.X and 2023.X.

**Context**:
Refer to Context
of [DEC-2024-09-12-002: Determine Supported Product Versions for the Plugin](#dec-2024-09-12-002-determine-supported-product-versions-for-the-plugin).
The goal is to support the latest IntelliJ Platform SDK functionality, with respect to CodeScene's plugin functionality
requirements, which is not available in versions earlier than 2023.X, including 2022.2, 2022.1, 2021.3, and 2021.2.

**Rationale**:
Refer to Rationale
of [DEC-2024-09-12-002: Determine Supported Product Versions for the Plugin](#dec-2024-09-12-002-determine-supported-product-versions-for-the-plugin).

**Consequences**:
The plugin needs to be tested and maintained for compatibility with IntelliJ IDEA 2023.X, and 2024.X.

**Alternatives Considered**:
None

**Related Decisions**:

- [DEC-2024-09-12-002: Determine Supported Product Versions for the Plugin](#dec-2024-09-12-002-determine-supported-product-versions-for-the-plugin)

## DEC-2024-10-04-001: Multiple CodeVision Providers for Distinct Code Smells

[Back to Top](#table-of-contents)

**Date**: 2024-10-04

**Decision Maker(s)**: Selma Copra

**Status**: Accepted

**Decision**:
To display multiple distinct code vision elements above problematic methods, a separate code vision provider was created
for each code smell category.

**Context**:
It was necessary to display distinct code vision entries above methods with detected code smells. However, it’s not
possible to display multiple entries from the same provider within the same text range. As a result, a distinct provider
had to be added for each code smell.

**Rationale**:
Using distinct providers allows each code smell to have its own independent code vision entry displayed above a method,
which resolves the issue where only the last entry would show up when multiple code smells were present.

**Consequences**:
Each code smell now requires its own provider, which increases the number of providers but ensures full functionality of
code vision entries.

**Alternatives Considered**:
None. Other solutions were incompatible with displaying multiple code vision entries within the same range from the same
provider.

**Additional Resources**:

- For more information, please refer to the support ticket on the JetBrains
  forum: [Why CodeVision entry display only the last entry for a given line/provider?](https://intellij-support.jetbrains.com/hc/en-us/community/posts/17654953887762-Why-CodeVision-entry-display-only-the-last-entry-for-a-given-line-provider) [date of access: October 2024]

## DEC-2024-12-23-001: Update Supported Product Versions for the Plugin

[Back to Top](#table-of-contents)

**Date**: 2024-12-23

**Decision Maker(s)**: Dzenan Granulo, Selma Copra

**Status**: Accepted

**Decision**:
The plugin will support IntelliJ IDEA versions from the last two major releases: 2024.X and 2023.3.

**Context**:
Refer to Context
of [DEC-2024-09-18-001: Update Supported Product Versions for the Plugin](#dec-2024-09-18-001-update-supported-product-versions-for-the-plugin).
The goal is to support the latest IntelliJ Platform SDK functionality, with respect to CodeScene's plugin functionality
requirements, which is not available in versions earlier than 2023.3, including 2023.2, 2023.1, 2022.2, 2022.1, 2021.3, and 2021.2.

**Rationale**:
Refer to Rationale
of [DEC-2024-09-18-001: Update Supported Product Versions for the Plugin](#dec-2024-09-18-001-update-supported-product-versions-for-the-plugin).

**Consequences**:
The plugin needs to be tested and maintained for compatibility with IntelliJ IDEA 2023.3, and 2024.X.

**Alternatives Considered**:
None

**Related Decisions**:

- [DEC-2024-09-18-001: Update Supported Product Versions for the Plugin](#dec-2024-09-18-001-update-supported-product-versions-for-the-plugin)

## DEC-2025-01-03-001: Code Smell Documentation Location Update

[Back to Top](#table-of-contents)

**Date**: 2025-01-03

**Decision Maker(s)**: Dzenan Granulo, Selma Copra

**Status**: Accepted

**Decision**:
The code smell documentation is to be moved to a common place (IDE Protocol repository), to streamline updates between both VS Code and JetBrains extensions. 

**Context**:
Previously, there was only the Visual Studio Code extension, so the documentation files were kept within its repository. Redundancy is now present since the extension is being implemented across several IDEs, requiring the duplication of documentation assets across repositories. To address this, it was decided to store these documentation files in a centralized location: IDE Protocol repository.

**Rationale**:
- Avoid redundancy by maintaining a single source of truth for documentation.
- Simplify updates by reducing the need for manual synchronization between two repositories.
- Enable potential future public access to documentation for broader community use.

**Consequences**:
To run the extension locally, users will need to ensure the documentation is populated by executing the `./gradlew fetchDocs` command. This process requires appropriate permissions to access the IDE Protocol repository.

**Alternatives Considered**:
None