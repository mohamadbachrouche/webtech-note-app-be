# 📝 Webtech Note App - Backend

A **Spring Boot REST API** for a modern note-taking application, developed for the Web Technologies course (WiSe 2025/26) at HTW Berlin.

## 🚀 Live Demo

| Service         | URL                                      |
| --------------- | ---------------------------------------- |
| **Backend API** | https://webtech-note-app-be.onrender.com |
| **Frontend**    | https://webtech-note-app-fe.onrender.com |

## 🛠️ Tech Stack

- **Framework:** Spring Boot 3.5.6
- **Language:** Java 25
- **Database:** PostgreSQL (Production) / H2 (Testing)
- **Build Tool:** Gradle
- **CI/CD:** GitHub Actions
- **Deployment:** Render.com (Dockerized)

## 📋 Features (8 Use Cases)

| Use Case           | Endpoint                    | Method |
| ------------------ | --------------------------- | ------ |
| Create Note        | `/api/notes`                | POST   |
| List All Notes     | `/api/notes`                | GET    |
| Get Single Note    | `/api/notes/{id}`           | GET    |
| Update Note        | `/api/notes/{id}`           | PUT    |
| Move to Trash      | `/api/notes/trash/{id}`     | PUT    |
| List Trashed Notes | `/api/notes/trash`          | GET    |
| Restore from Trash | `/api/notes/restore/{id}`   | PUT    |
| Permanent Delete   | `/api/notes/permanent/{id}` | DELETE |

## 🏗️ Architecture

```
3-Tier Architecture
├── Controller Layer (REST API)
├── Service Layer (Business Logic)
└── Repository Layer (Data Access)
```

## 🧪 Running Locally

```bash
# Clone the repository
git clone https://github.com/mohamadbachrouche/webtech-note-app-be.git
cd webtech-note-app-be

# Run with Gradle (requires Java 25)
./gradlew bootRun

# Run tests
./gradlew test
```

> **Note:** Set the `JDBC_DATABASE_URL` environment variable for database connection.

## 📦 Related Repositories

- **Frontend:** [webtech-note-app-fe](https://github.com/mohamadbachrouche/webtech-note-app-fe)

---

_Developed by Mohamad Bachrouche • HTW Berlin • 2025_
