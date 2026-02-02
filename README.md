# MBT School Management Bot

MBT School Management Bot is a Telegram-based communication and data management system designed for schools. It provides a streamlined interface for parents, students, and teachers to interact with school data in real time.

The bot integrates Firebase Firestore as the primary database and supports automated user synchronization from Google Sheets. It includes role-based menus, secure login via phone number verification, homework distribution, grade management, and notification broadcasting.

## Features

### 1. Role-Based Login

Users authenticate by sending their phone number. Their role (parent, student, or teacher) is determined automatically based on records stored in Firebase.

### 2. Parent Panel

Parents can:

* View their child's grades with date-based pagination.
* View homework assigned to their child’s class.
* Access teacher contact information.
* Log out from the system.

### 3. Student Panel

Students can:

* View homework assigned to their class.
* Log out safely.

### 4. Teacher Panel

Teachers can:

* Assign grades to students via a guided selection flow (class → subject → student).
* Assign homework with subject and deadline selection.
* Post announcements/notices to specific classes.
  All students in that class receive the notification instantly.
* Log out from the system.

### 5. Grade Management

Grades are stored with:

* Student phone number
* Teacher phone number
* Subject
* Score (supports numbers and ABS)
* Timestamp

Parents can view grades grouped by date, with next/previous page navigation.

### 6. Homework Management

Teachers can assign homework with:

* Class
* Subject
* Text description
* Deadline (autoconverted to milliseconds)

Students and parents can view structured homework lists sorted by date.

### 7. Notifications System

Teachers can send announcements to entire classes.
The bot:

* Saves the notification in Firebase
* Sends it automatically to every student logged in the class

### 8. Google Sheets Synchronization

The bot can sync users from Google Sheets into Firebase.
Supported sheets:

* Parents
* Students
* Teachers

Each sheet syncs only missing users, avoiding duplicates.

Sync commands (restricted to admin):

* `/syncParents`
* `/syncStudents`
* `/syncTeachers`
* `/sync` (full sync)

### 9. Firebase Integration

Uses Firestore for:

* User storage
* Grades
* Homework
* Notifications

Timestamps are stored consistently in milliseconds.

### 10. Inline Navigation and Dynamic UI

Uses Telegram InlineKeyboard for:

* Class selection
* Subject selection
* Student selection
* Grade pagination
* Homework and notification flows

---

## Technologies Used

* Java 17+
* Telegram Bot API
* Firebase Admin SDK
* Google Sheets API
* Maven
* Firestore NoSQL Database

---

## Purpose

This project is designed to streamline communication between schools, students, parents, and teachers. It centralizes grade reporting, homework distribution, parent–teacher communication, and important announcements into a single automated bot accessible via Telegram.

