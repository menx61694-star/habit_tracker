import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:table_calendar/table_calendar.dart';
import 'notification_service.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await NotificationService.init();
  await NotificationService.scheduleDailyReminder();
  runApp(const HabitApp());
}

class HabitApp extends StatelessWidget {
  const HabitApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        colorSchemeSeed: Colors.indigo,
      ),
      home: const HabitScreen(),
    );
  }
}

class HabitScreen extends StatefulWidget {
  const HabitScreen({super.key});

  @override
  State<HabitScreen> createState() => _HabitScreenState();
}

class _HabitScreenState extends State<HabitScreen> {
  String habit = "Set your habit";
  Map<String, bool> logs = {};
  DateTime focusedDay = DateTime.now();

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  String _dateKey(DateTime d) => "${d.year}-${d.month}-${d.day}";

  Future<void> _loadData() async {
    final prefs = await SharedPreferences.getInstance();
    habit = prefs.getString("habit") ?? habit;

    final raw = prefs.getString("logs");
    if (raw != null) {
      logs = Map<String, bool>.from(jsonDecode(raw));
    }
    setState(() {});
  }

  Future<void> _saveData() async {
    final prefs = await SharedPreferences.getInstance();
    prefs.setString("habit", habit);
    prefs.setString("logs", jsonEncode(logs));
  }

  void _toggleDay(DateTime day) {
    final key = _dateKey(day);
    logs[key] = !(logs[key] ?? false);
    _saveData();
    setState(() {});
  }

  int _streak() {
    int count = 0;
    DateTime d = DateTime.now();
    while (logs[_dateKey(d)] == true) {
      count++;
      d = d.subtract(const Duration(days: 1));
    }
    return count;
  }

  void _editHabit() {
    final controller = TextEditingController(text: habit);
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text("Edit Habit"),
        content: TextField(controller: controller),
        actions: [
          TextButton(
            onPressed: () {
              habit = controller.text.trim().isEmpty
                  ? habit
                  : controller.text.trim();
              _saveData();
              setState(() {});
              Navigator.pop(context);
            },
            child: const Text("Save"),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Habit Tracker"),
        actions: [
          IconButton(
            icon: const Icon(Icons.edit),
            onPressed: _editHabit,
          ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              habit,
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            const SizedBox(height: 8),
            Text("🔥 Streak: ${_streak()} days"),
            const SizedBox(height: 16),

            TableCalendar(
              firstDay: DateTime.utc(2022, 1, 1),
              lastDay: DateTime.utc(2035, 12, 31),
              focusedDay: focusedDay,
              selectedDayPredicate: (day) =>
                  logs[_dateKey(day)] == true,
              onDaySelected: (selectedDay, focused) {
                focusedDay = focused;
                _toggleDay(selectedDay);
              },
              calendarBuilders: CalendarBuilders(
                defaultBuilder: (context, day, _) {
                  final done = logs[_dateKey(day)] ?? false;
                  return Container(
                    margin: const EdgeInsets.all(4),
                    decoration: BoxDecoration(
                      color: done ? Colors.green : Colors.grey.shade300,
                      shape: BoxShape.circle,
                    ),
                    alignment: Alignment.center,
                    child: Text(
                      "${day.day}",
                      style: const TextStyle(color: Colors.white),
                    ),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}
