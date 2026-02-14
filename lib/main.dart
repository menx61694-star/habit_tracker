import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'notification_service.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await NotificationService.init();
  await NotificationService.scheduleDailyReminder();
  runApp(const HabitApp());
}

class HabitApp extends StatefulWidget {
  const HabitApp({super.key});

  @override
  State<HabitApp> createState() => _HabitAppState();
}

class _HabitAppState extends State<HabitApp> {
  ThemeMode _themeMode = ThemeMode.light;

  void _updateTheme(ThemeMode mode) {
    setState(() {
      _themeMode = mode;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      themeMode: _themeMode,
      theme: ThemeData(
        useMaterial3: true,
        colorSchemeSeed: Colors.indigo,
      ),
      darkTheme: ThemeData(
        useMaterial3: true,
        brightness: Brightness.dark,
        colorSchemeSeed: Colors.indigo,
      ),
      home: HabitHome(onThemeChanged: _updateTheme),
    );
  }
}

class Habit {
  Habit({
    required this.id,
    required this.name,
    required this.note,
    Map<String, bool>? logs,
    this.createdAt,
  }) : logs = logs ?? {};

  final String id;
  final String name;
  final String note;
  final DateTime? createdAt;
  final Map<String, bool> logs;

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'note': note,
      'createdAt': createdAt?.toIso8601String(),
      'logs': logs,
    };
  }

  static Habit fromJson(Map<String, dynamic> json) {
    return Habit(
      id: json['id'] as String,
      name: json['name'] as String,
      note: json['note'] as String? ?? '',
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'] as String)
          : null,
      logs: Map<String, bool>.from(json['logs'] as Map? ?? {}),
    );
  }
}

class HabitHome extends StatefulWidget {
  const HabitHome({super.key, required this.onThemeChanged});

  final ValueChanged<ThemeMode> onThemeChanged;

  @override
  State<HabitHome> createState() => _HabitHomeState();
}

class _HabitHomeState extends State<HabitHome> {
  static const List<String> _habitSuggestions = [
    'Drink 8 glasses of water',
    'Read for 20 minutes',
    'Walk 7,000 steps',
    'Meditate for 10 minutes',
    'Sleep before 11 PM',
    'Practice gratitude journaling',
  ];

  final TextEditingController _feedbackController = TextEditingController();
  final List<Habit> _habits = [];
  final List<String> _feedbackEntries = [];

  int _selectedTab = 0;
  bool _isDarkMode = false;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  @override
  void dispose() {
    _feedbackController.dispose();
    super.dispose();
  }

  String _dateKey(DateTime date) {
    return '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
  }

  Future<void> _loadData() async {
    final prefs = await SharedPreferences.getInstance();
    final rawHabits = prefs.getString('habits');
    final rawFeedback = prefs.getStringList('feedback') ?? [];
    final darkMode = prefs.getBool('isDarkMode') ?? false;

    if (rawHabits != null) {
      final decoded = jsonDecode(rawHabits) as List<dynamic>;
      _habits
        ..clear()
        ..addAll(decoded.map((item) => Habit.fromJson(item as Map<String, dynamic>)));
    }

    _feedbackEntries
      ..clear()
      ..addAll(rawFeedback);

    _isDarkMode = darkMode;
    widget.onThemeChanged(_isDarkMode ? ThemeMode.dark : ThemeMode.light);
    setState(() {});
  }

  Future<void> _saveData() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('habits', jsonEncode(_habits.map((h) => h.toJson()).toList()));
    await prefs.setBool('isDarkMode', _isDarkMode);
    await prefs.setStringList('feedback', _feedbackEntries);
  }

  void _addHabit(String name, {String note = ''}) {
    if (name.trim().isEmpty) {
      return;
    }

    setState(() {
      _habits.add(
        Habit(
          id: DateTime.now().microsecondsSinceEpoch.toString(),
          name: name.trim(),
          note: note.trim(),
          createdAt: DateTime.now(),
        ),
      );
    });
    _saveData();
  }

  void _removeHabit(String id) {
    setState(() {
      _habits.removeWhere((habit) => habit.id == id);
    });
    _saveData();
  }

  void _toggleToday(Habit habit, bool? checked) {
    final key = _dateKey(DateTime.now());
    setState(() {
      habit.logs[key] = checked ?? false;
    });
    _saveData();
  }

  int _completedCountForDate(DateTime date) {
    final key = _dateKey(date);
    return _habits.where((habit) => habit.logs[key] == true).length;
  }

  double _monthlyCompletionRate(DateTime month) {
    if (_habits.isEmpty) {
      return 0;
    }
    final daysInMonth = DateTime(month.year, month.month + 1, 0).day;
    var totalPossible = 0;
    var totalDone = 0;

    for (var day = 1; day <= daysInMonth; day++) {
      final date = DateTime(month.year, month.month, day);
      totalPossible += _habits.length;
      totalDone += _completedCountForDate(date);
    }

    return totalPossible == 0 ? 0 : (totalDone / totalPossible) * 100;
  }

  double _yearlyCompletionRate(DateTime year) {
    if (_habits.isEmpty) {
      return 0;
    }

    var totalPossible = 0;
    var totalDone = 0;

    for (var month = 1; month <= 12; month++) {
      final daysInMonth = DateTime(year.year, month + 1, 0).day;
      for (var day = 1; day <= daysInMonth; day++) {
        totalPossible += _habits.length;
        totalDone += _completedCountForDate(DateTime(year.year, month, day));
      }
    }

    return totalPossible == 0 ? 0 : (totalDone / totalPossible) * 100;
  }

  void _showAddHabitDialog({String suggestion = ''}) {
    final nameController = TextEditingController(text: suggestion);
    final noteController = TextEditingController();

    showDialog<void>(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text('Add Habit'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: nameController,
                decoration: const InputDecoration(labelText: 'Habit name'),
              ),
              const SizedBox(height: 10),
              TextField(
                controller: noteController,
                decoration: const InputDecoration(labelText: 'Note (optional)'),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Cancel'),
            ),
            FilledButton(
              onPressed: () {
                _addHabit(nameController.text, note: noteController.text);
                Navigator.pop(context);
              },
              child: const Text('Add'),
            ),
          ],
        );
      },
    );
  }

  void _saveFeedback() {
    if (_feedbackController.text.trim().isEmpty) {
      return;
    }
    setState(() {
      _feedbackEntries.add(_feedbackController.text.trim());
      _feedbackController.clear();
    });
    _saveData();
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Thanks for your feedback!')),
    );
  }

  Widget _buildHomePage() {
    final todayDone = _completedCountForDate(DateTime.now());
    final total = _habits.length;

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text(
          'Home',
          style: Theme.of(context).textTheme.headlineMedium,
        ),
        const SizedBox(height: 8),
        Text(
          'Track your daily habits and stay consistent.',
          style: Theme.of(context).textTheme.bodyLarge,
        ),
        const SizedBox(height: 16),
        Card(
          child: ListTile(
            leading: const Icon(Icons.today),
            title: const Text('Today progress'),
            subtitle: Text('$todayDone of $total habits completed'),
          ),
        ),
        const SizedBox(height: 12),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Today habit checklist',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const SizedBox(height: 10),
                if (_habits.isEmpty)
                  const Text('No habits yet. Add one from Habits tab.')
                else
                  ..._habits.map((habit) {
                    final checked = habit.logs[_dateKey(DateTime.now())] ?? false;
                    return CheckboxListTile(
                      dense: true,
                      contentPadding: EdgeInsets.zero,
                      value: checked,
                      title: Text(habit.name),
                      subtitle: habit.note.isEmpty ? null : Text(habit.note),
                      onChanged: (value) => _toggleToday(habit, value),
                    );
                  }),
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        Text(
          'Habit suggestions',
          style: Theme.of(context).textTheme.titleMedium,
        ),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: _habitSuggestions
              .map(
                (suggestion) => ActionChip(
                  label: Text(suggestion),
                  onPressed: () => _showAddHabitDialog(suggestion: suggestion),
                ),
              )
              .toList(),
        ),
      ],
    );
  }

  Widget _buildHabitsPage() {
    return Column(
      children: [
        Expanded(
          child: _habits.isEmpty
              ? const Center(child: Text('No habits added yet.'))
              : ListView.builder(
                  itemCount: _habits.length,
                  itemBuilder: (context, index) {
                    final habit = _habits[index];
                    return Card(
                      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
                      child: ListTile(
                        title: Text(habit.name),
                        subtitle: Text(habit.note.isEmpty ? 'No note' : habit.note),
                        trailing: IconButton(
                          icon: const Icon(Icons.delete_outline),
                          onPressed: () => _removeHabit(habit.id),
                        ),
                      ),
                    );
                  },
                ),
        ),
        Padding(
          padding: const EdgeInsets.all(16),
          child: SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              onPressed: _showAddHabitDialog,
              icon: const Icon(Icons.add),
              label: const Text('Add habit'),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildReportsPage() {
    final now = DateTime.now();
    final daily = _completedCountForDate(now);
    final monthlyRate = _monthlyCompletionRate(now);
    final yearlyRate = _yearlyCompletionRate(now);

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text('Reports', style: Theme.of(context).textTheme.headlineMedium),
        const SizedBox(height: 14),
        Card(
          child: ListTile(
            leading: const Icon(Icons.calendar_view_day),
            title: const Text('Daily report'),
            subtitle: Text('Completed today: $daily of ${_habits.length}'),
          ),
        ),
        Card(
          child: ListTile(
            leading: const Icon(Icons.calendar_view_month),
            title: const Text('Monthly report'),
            subtitle: Text('Completion rate: ${monthlyRate.toStringAsFixed(1)}%'),
          ),
        ),
        Card(
          child: ListTile(
            leading: const Icon(Icons.calendar_today),
            title: const Text('Yearly report'),
            subtitle: Text('Completion rate: ${yearlyRate.toStringAsFixed(1)}%'),
          ),
        ),
      ],
    );
  }

  Widget _buildSettingsPage() {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text('Settings', style: Theme.of(context).textTheme.headlineMedium),
        const SizedBox(height: 10),
        SwitchListTile(
          title: const Text('Dark theme'),
          subtitle: const Text('Toggle app appearance'),
          value: _isDarkMode,
          onChanged: (value) {
            setState(() {
              _isDarkMode = value;
            });
            widget.onThemeChanged(value ? ThemeMode.dark : ThemeMode.light);
            _saveData();
          },
        ),
        const Divider(height: 24),
        TextField(
          controller: _feedbackController,
          maxLines: 4,
          decoration: const InputDecoration(
            border: OutlineInputBorder(),
            labelText: 'Feedback',
            hintText: 'Share your experience and suggestions...',
          ),
        ),
        const SizedBox(height: 10),
        FilledButton(
          onPressed: _saveFeedback,
          child: const Text('Submit feedback'),
        ),
        const SizedBox(height: 12),
        Text('Recent feedback (${_feedbackEntries.length})'),
        const SizedBox(height: 8),
        ..._feedbackEntries.reversed.take(3).map(
              (entry) => Card(
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Text(entry),
                ),
              ),
            ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    final pages = [
      _buildHomePage(),
      _buildHabitsPage(),
      _buildReportsPage(),
      _buildSettingsPage(),
    ];

    return Scaffold(
      appBar: AppBar(
        title: const Text('Habit Tracker'),
      ),
      body: pages[_selectedTab],
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selectedTab,
        onDestinationSelected: (value) {
          setState(() {
            _selectedTab = value;
          });
        },
        destinations: const [
          NavigationDestination(icon: Icon(Icons.home_outlined), label: 'Home'),
          NavigationDestination(icon: Icon(Icons.checklist_rtl), label: 'Habits'),
          NavigationDestination(icon: Icon(Icons.bar_chart), label: 'Reports'),
          NavigationDestination(icon: Icon(Icons.settings_outlined), label: 'Settings'),
        ],
      ),
    );
  }
}
