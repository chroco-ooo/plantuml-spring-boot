# plantuml-spring-boot

## Overview
`plantuml-spring-boot` is a web application built with Spring Boot for generating and serving UML diagrams using PlantUML. This project aims to provide a platform for easily creating and sharing UML diagrams.

## Features
- Easy setup and execution with Spring Boot
- Generation of UML diagrams using PlantUML
- Generation and retrieval of UML diagrams through REST API

## Prerequisites
- Java 17
- Maven

## Setup Instructions
1. Clone the repository.
   ```
   git clone https://github.com/chroco-ooo/plantuml-spring-boot.git
   ```
2. Navigate to the project directory.
   ```
   cd plantuml-spring-boot
   ```
3. Use Maven to build the project.
   ```
   mvn clean install
   ```
4. Start the Spring Boot application.
   ```
   mvn spring-boot:run
   ```

## How to Use
Once the application is running, you can generate UML diagrams by constructing a URL with the following format:

```
http://localhost:8080/{type}/{encoded-text}
```

Where `{type}` is the type of diagram you want to generate, and `{encoded-text}` is your PlantUML diagram description that has been text-encoded. Text encoding is necessary to ensure that the diagram description is properly transmitted via URL.

### Text Encoding
To encode your PlantUML description, you can follow the text encoding instructions provided by PlantUML. This process converts your PlantUML text into a compressed format suitable for URL usage. For detailed guidance on text encoding, please refer to the PlantUML documentation:

[PlantUML Text Encoding](https://plantuml.com/ja/text-encoding)

### Example
If you have a PlantUML description for a simple class diagram, you first need to text-encode this description. Once encoded, you construct the URL by replacing `{type}` with your diagram type (e.g., `uml`) and `{encoded-text}` with your encoded PlantUML text.

Accessing this URL in a web browser or through a REST client will generate and display the UML diagram.

## How to Use
Once the application is running, you can generate UML diagrams by accessing the following endpoint:

- UML Diagram Generation API: `POST /api/uml/generate`

Send a request body containing your PlantUML description. Upon success, the generated UML diagram image or URL will be returned.

## Contributing
Contributions to this project are welcome. Please feel free to contribute through bug reports, feature suggestions, and pull requests to help improve the project.

## License
This project is made available under the [MIT License](LICENSE).

## Acknowledgments
- [Spring Boot](https://spring.io/projects/spring-boot) for the server-side framework
- [PlantUML](http://plantuml.com/) for the UML diagram generation tool
