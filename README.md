# 💾 My-Own-Git - A Simple Git-like Version Control System

A lightweight version control system (VCS) built from scratch in **Java**, designed to replicate the core functionality of Git. This project is an **educational tool** that demystifies Git internals, including content-addressable storage, SHA-1 hashing, and object models like blobs, trees, and commits.

---

## 🚀 Features

- 🗃️ `init` — Initialize a new repository (`.mygit`)
- ➕ `add <file>` — Stage a file (creates blob object)
- 📝 `commit -m "<message>"` — Save a snapshot of the current state
- 📜 `log` — View the commit history

---

## 🔧 How It Works

MyGit uses a simplified object model stored in `.mygit/objects/`, inspired by Git:

- 📄 **Blob** — Stores raw file content using SHA-1 hash as the identifier
- 📁 **Tree** — Represents directory structure with pointers to blobs/trees
- 🧱 **Commit** — Points to a tree and parent commit(s); stores metadata

---

## 🛠️ How to Compile & Use

### ⚙️ Prerequisites
- Java JDK 8 or higher ☕
- Terminal / Command Prompt access

---
## 📁 File Usage: Step-by-Step Guide

### 1️⃣ Compile MyGit.java

```bash
# Navigate to your tool directory
cd C:\Users\YourUser\Desktop\My-Own-Git

# Compile the tool
javac MyGit.java
```
### 2️⃣ Initialize in Your Project
```bash
# Go to the project directory
cd C:\Users\YourUser\Desktop\my-project

# Initialize a new MyGit repo
java -cp ..\My-Own-Git MyGit init
```
### 3️⃣ Add and Commit Files
```bash
# Create and stage a file
echo "Hello, World!" > readme.md
java -cp ..\My-Own-Git MyGit add readme.md

# Commit the change
java -cp ..\My-Own-Git MyGit commit -m "Initial commit: Add readme file"
```
### 4️⃣ Add More Files
```
echo "My second file" > notes.txt
java -cp ..\My-Own-Git MyGit add notes.txt
java -cp ..\My-Own-Git MyGit commit -m "Add notes.txt"
```
### 5️⃣ View Commit History
```
java -cp ..\My-Own-Git MyGit log
```

###🧠 Key Learnings
-🔐 SHA-1 Hashing for content tracking
-📦 Java Object Serialization for storing Git-like objects
-📁 File I/O using java.nio.file
-🌳 Tree/Commit Graph structures

###✨ Future Improvements
-status — View staging and working directory status
-checkout — Switch between commits or branches
-branch — Create and manage branches
-diff — View differences between file versions
-🗂️ Add recursive directory support
