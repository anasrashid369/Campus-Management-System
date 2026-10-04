# Campus Management System (SCD_A1)

A CLI-based university management system for handling courses, sections, students, instructors, and FYP projects.

## Run

```powershell
javac -d bin -encoding UTF-8 src\*.java
java -cp bin Main
```

Persistence files are written to `data/catalog.txt`. Application logs are written to `logs/app.log`.

## Design Changes: Section-Course Decoupling

### Problem Fixed

Previously, the system had a critical design flaw:
- Sections were tightly coupled to a single course at creation time
- This was logically wrong: a **Section (e.g., "BSE-5B")** in a real university is a **cohort that takes multiple courses**

### Solution Implemented

Sections are now **decoupled from courses**:

#### Before (Incorrect)
```
Create Section → Specify Course Code → Section tied to one course
Logic: Section → (single) Course ❌
```

#### After (Correct)
```
Create Section → (independent) → Assign Courses Later
Logic: Section → [multiple] Courses ✓
```

## What Changed

### Core Domain
- **Section.java**
  - Constructor no longer requires a `Course` parameter
  - Replaced `private Course course` with `private List<Course> courses`
  - Added `addCourse()`, `removeCourse()`, `getCourses()`, `getPrimaryCourse()`

- **AcademicOfficeAdmin.java**
  - Renamed `createSection()` to work without course requirement
  - Added `assignCourseToSection()` - Link course to section
  - Added `updateCourseInSection()` - Modify course in section
  - Added `removeCourseFromSection()` - Unlink course
  - Added `deleteSection()` - Remove section with cleanup

### CLI Workflows (AdminCliHandler)
- **Create Section** - Now asks only for Section ID and Capacity
- **Assign Course to Section** - NEW option to link courses after section creation
- **Update Course in Section** - NEW option to modify courses in section
- **Remove Course from Section** - NEW option to unlink courses
- **Delete Section** - NEW option with course cleanup

### Dependent Classes Updated
- **Enrollment.java** - Uses `getPrimaryCourse()` for backward compatibility
- **Student.java** - Register/drop now handle multiple courses gracefully
- **Instructor.java** - `viewCourses()` iterates all courses in each section
- **TeachingAssistant.java** - Assignment creation handles no primary course
- **CourseClashRequest.java** - Safely displays courses when available
- **InstructorCliHandler.java** - Shows all courses in section
- **TeachingAssistantCliHandler.java** - Uses courses safely
- **CampusPersistence.java** - Loads/saves sections independently

## Academic Officer Menu

```
1.  Create Course
2.  Update Course
3.  Search Course
4.  Create Section                    [CHANGED - no course required]
5.  Update Section
6.  Set Section Capacity
7.  Assign Room
8.  Assign Instructor
9.  Assign Course to Section          [NEW]
10. Update Course in Section          [NEW]
11. Remove Course from Section        [NEW]
12. Delete Section                    [NEW]
13. View Requests
14. Approve Request
15. Reject Request
```

## Typical Usage Flow

### 1. Create a Course
```
Menu: 1. Create Course
→ Course code: CS101
→ Course title: Introduction to Programming
→ Credit hours: 3
```

### 2. Create a Section
```
Menu: 4. Create Section
→ Section ID: BSE-5B
→ Section capacity: 40
✓ Section BSE-5B created successfully. Courses can be assigned later.
```

### 3. Assign Courses to Section
```
Menu: 9. Assign Course to Section
→ Section ID: BSE-5B
→ Course code: CS101
✓ Course CS101 assigned to section BSE-5B.

(Repeat for more courses)
```

### 4. Update Section Details
```
Menu: 7. Assign Room
→ Section ID: BSE-5B
→ Day: Monday
→ Start time: 09:00
→ End time: 11:00
→ Room: Room 101
✓ Room assigned to section BSE-5B: Monday 09:00-11:00 in Room 101
```

### 5. Manage Courses
```
Menu: 10. Update Course in Section
→ Section ID: BSE-5B
→ Course code to update: CS101
→ New course title: Advanced Programming
→ New credit hours: 4
✓ Course CS101 updated in section BSE-5B.

Menu: 11. Remove Course from Section
→ Section ID: BSE-5B
→ Course code to remove: CS101
✓ Course CS101 removed from section BSE-5B.
```

### 6. Delete a Section
```
Menu: 12. Delete Section
→ Section ID: BSE-5B
✓ Section BSE-5B deleted successfully.
```

## Error Handling

The system validates all inputs and provides clear feedback:
- "Section X already exists" → Cannot create duplicate sections
- "No course found with code Y" → Course must exist before assigning
- "Course is already assigned to this section" → Prevents duplicates
- "Course is not assigned to this section" → For updates/removals
- "This section has no courses assigned" → For operations requiring courses

## Backward Compatibility

- The persistence file format (`data/catalog.txt`) still stores one course code per section for compatibility
- The system uses `getPrimaryCourse()` as a fallback for single-course access
- Sections with no courses are handled gracefully throughout the codebase
- Existing catalog files load and save correctly

## Data Format

### SECTION Record (data/catalog.txt)
```
SECTION <sectionId> <courseCode> <capacity> <day> <startTime> <endTime> <room>
```

Example:
```
SECTION BSE-5B CS101 40 Monday 09:00:00 11:00:00 Room101
```

Note: Only the first/primary course is persisted in the current format. Additional courses assigned via the UI are stored in memory.

## Architecture

```
Campus Management System
├── Entities
│   ├── Section (decoupled from Course)
│   ├── Course
│   ├── Enrollment
│   ├── Student / Instructor
│   └── ...
├── Operations
│   └── AcademicOfficeAdmin (orchestrates all operations)
├── CLI Interface
│   ├── AdminCliHandler (menu + workflows)
│   ├── StudentCliHandler
│   ├── InstructorCliHandler
│   └── ...
├── Persistence
│   └── CampusPersistence (loads/saves data)
└── Data
    └── data/catalog.txt
```

## Key Improvements

✅ **Logical Design** - Sections now correctly represent cohorts with multiple courses  
✅ **Flexibility** - Courses can be assigned/removed after section creation  
✅ **Validation** - Prevents invalid operations with clear error messages  
✅ **Clean Architecture** - Separation of concerns between domain, CLI, and persistence  
✅ **Backward Compatible** - Existing code and data formats still work  

## Development Notes

- Pure Java CLI application, no external dependencies
- Tab-separated values (TSV) format for persistence
- Loop-based interactive menu system
- All classes follow single-responsibility principle
- Full logging via ApplicationLogger to `logs/app.log`

Generated with [Claude Code](https://claude.com/claude-code)
