### 💡 O que é e o que ele faz
Uma aplicação de Lista de Tarefas com interface moderna em JavaFX (tema escuro e PT-BR), permitindo criar, editar, marcar como concluídas e remover tarefas. Os dados são mantidos de forma simples e confiável em arquivo JSON, com integração ao backend Spring Boot via API REST. ✨

### ✅ Requisitos
- Windows com JDK 21 (ou JDK 17+) instalado
- jpackage disponível (somente se for gerar instalador para Windows)

### 🧰 Tecnologias usadas
- JavaFX 21 (controles, FXML e CSS)
- Spring Boot 3 (API REST)
- Jackson (serialização e persistência em JSON)
- Maven (build, empacotamento e gerenciamento de dependências — Maven embarcado incluído)
- SLF4J (logs)
- YAML para configuração de ambiente

### 🚀 Instalação
- Clone ou baixe este repositório
- Build com Maven embarcado (recomendado):
  - Abra um terminal no diretório do projeto
  - Execute `.\tools\apache-maven-3.9.9\bin\mvn.cmd clean package -DskipTests`
  - Artefatos gerados: `backend\target\backend-1.0.0.jar` e `frontend\target\frontend-1.0.0-shaded.jar`
- Instalador Windows (opcional):
  - Requer `jpackage` disponível no JDK
  - Se já gerado, está em `frontend\target\installer\ToDoList`
  - Para gerar um instalador simples: `jpackage --name ToDoList --input .\frontend\target --main-jar frontend-1.0.0-shaded.jar --main-class com.example.todolist.frontend.Launcher --type exe`

### ▶️ Execução
- Inicie primeiro o backend:
  - Via Maven: `.\tools\apache-maven-3.9.9\bin\mvn.cmd -pl backend spring-boot:run`
  - Ou via JAR: `java -jar .\backend\target\backend-1.0.0.jar`
  - API em `http://localhost:8080` e dados em `backend\data\tasks.json`
- Depois, execute o frontend:
  - Via Maven: `.\tools\apache-maven-3.9.9\bin\mvn.cmd -pl frontend javafx:run`
  - Ou via JAR: `java -jar .\frontend\target\frontend-1.0.0-shaded.jar`
- Se instalou via `jpackage`, use o atalho instalado no Windows
