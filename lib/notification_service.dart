import 'package:flutter_local_notifications/flutter_local_notifications.dart';

class NotificationService {
  static final FlutterLocalNotificationsPlugin _notifications =
      FlutterLocalNotificationsPlugin();

  static Future<void> init() async {
    const AndroidInitializationSettings androidInit =
        AndroidInitializationSettings('@mipmap/ic_launcher');

    const InitializationSettings initSettings =
        InitializationSettings(android: androidInit);

    await _notifications.initialize(initSettings);
  }

  static Future<void> scheduleDailyReminder() async {
    const AndroidNotificationDetails androidDetails =
        AndroidNotificationDetails(
      'habit_channel',
      'Habit Reminder',
      channelDescription: 'Daily habit reminder',
      importance: Importance.high,
      priority: Priority.high,
    );

    const NotificationDetails details =
        NotificationDetails(android: androidDetails);

    await _notifications.showDailyAtTime(
      0,
      'Habit Reminder',
      'Aaj habit complete kiya ya nahi?',
      const Time(20, 0, 0), // 8:00 PM
      details,
      androidAllowWhileIdle: true,
    );
  }
}
