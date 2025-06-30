<<<<<<< HEAD
# FocusBuddy - Productivity Assistant

A comprehensive JavaFX desktop application designed to help students manage their productivity through task management, mood tracking, note-taking, goal setting, and Pomodoro timer functionality.

## Features

### 🔐 Security & Authentication
- Secure user registration and login
- Password hashing with salt
- Input validation and sanitization
- SQL injection protection

### ✅ Task Management
- Create, edit, and delete tasks
- Priority levels (High, Medium, Low)
- Status tracking (Pending, In Progress, Completed)
- Due date management
- Advanced filtering and sorting
- Search functionality

### 🍅 Pomodoro Timer
- 25-minute focus sessions
- 5-minute break intervals
- Visual progress tracking
- Timer notifications
- Session statistics

### 😊 Mood Tracker
- Daily mood logging (1-5 scale)
- Mood history with visual charts
- Streak tracking
- Mood statistics and trends

### 📝 Smart Notes
- Rich text editor with HTML support
- Text formatting (Bold, Italic, Highlight)
- Decorator pattern implementation
- Search and tagging system
- Auto-save functionality

### 🎯 Goals Tracker
- Multiple goal types (Study Hours, Focus Sessions, Tasks)
- Progress tracking with visual indicators
- Achievement system with badges
- Goal statistics and completion tracking

### 💾 Export & Backup
- CSV export for all data types
- Database backup functionality
- Summary reports generation
- Async operations with progress tracking

### 🎨 UI/UX
- Modern, responsive design
- Dark/Light theme toggle
- Professional styling
- Smooth animations
- Intuitive navigation

## Technical Architecture

### Design Patterns
- **Singleton**: UserSession, DatabaseManager, ThemeManager, NotificationManager
- **Factory**: User creation system
- **Observer**: Timer events, Goal completion notifications
- **Decorator**: Note formatting system
- **Abstract Classes**: User, Goal with concrete implementations

### Technologies Used
- **JavaFX 17**: UI framework
- **MySQL 8.0**: Database
- **Maven**: Build management
- **JUnit 5**: Unit testing
- **CSS**: Styling

## Installation

### Prerequisites
- Java 11 or higher
- MySQL 8.0 or higher
- Maven 3.6 or higher

### Setup
1. Clone the repository
2. Install MySQL and create a database named `focusbuddy`
3. Update database credentials in `DatabaseManager.java` if needed
4. Run `mvn clean install` to build the project
5. Run `mvn javafx:run` to start the application

### Default Login
- Username: `demo`
- Password: `demo123`

## Database Schema

The application automatically creates the following tables:
- `users` - User accounts and authentication
- `tasks` - Task management data
- `mood_entries` - Daily mood tracking
- `notes` - User notes and content
- `goals` - Goal tracking and progress
- `focus_sessions` - Pomodoro timer sessions
- `migrations` - Database version control

## Configuration

The application uses `focusbuddy.properties` for configuration:
- Database connection settings
- Timer durations
- Theme preferences
- Notification settings

## Testing

Run unit tests with:
\`\`\`bash
mvn test
\`\`\`

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

This project is licensed under the MIT License.

## Support

For support or questions, please create an issue in the repository.
=======
# testing-3
project pbo
>>>>>>> f91d21dc04c6402f3795ebda05eab518fce07018
