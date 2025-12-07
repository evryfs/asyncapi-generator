# Contributing to AsyncAPI Generator

Thank you for your interest in contributing to the AsyncAPI Generator project! We welcome contributions from the community to help make this tool better.

## Getting Started

1.  **Fork the Repository:** Click the "Fork" button on the top right of the repository page.
2.  **Clone Your Fork:**
    ```bash
    git clone https://github.com/YOUR_USERNAME/asyncapi-generator-core.git
    cd asyncapi-generator-core
    ```
3.  **Set Up Environment:** Ensure you have Java 21+ and Maven installed.
    ```bash
    mvn clean install
    ```

## Development Workflow

### Project Structure
- `asyncapi-generator-core`: The core library containing the parser, model, and generator logic.
- `asyncapi-generator-cli`: The Command Line Interface wrapper.
- `asyncapi-generator-maven-plugin`: The Maven plugin integration.

### Code Style
- We follow standard Kotlin coding conventions.
- Please ensure your code is formatted correctly before submitting.

### Testing
- **Unit Tests:** We heavily rely on unit tests. Please add tests for any new features or bug fixes.
- **Run Tests:**
    ```bash
    mvn test
    ```

## How to Contribute

### Reporting Bugs
If you find a bug, please open an issue on GitHub. Include:
- A clear description of the issue.
- A minimal reproduction (e.g., a sample AsyncAPI YAML file).
- Steps to reproduce.

### Suggesting Enhancements
We love new ideas! Open an issue to discuss your proposal before starting implementation.

### Submitting Pull Requests
1.  Create a new branch for your feature or fix: `git checkout -b feature/my-new-feature`
2.  Commit your changes: `git commit -am 'Add some feature'`
3.  Push to the branch: `git push origin feature/my-new-feature`
4.  Open a Pull Request against the `main` branch.
