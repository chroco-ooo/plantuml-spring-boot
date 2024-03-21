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
   git clone https://github.com/yourusername/plantuml-spring-boot.git
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
