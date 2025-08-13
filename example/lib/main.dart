import 'dart:async';

import 'package:flutter/material.dart';
import 'package:aeyrium_sensor/aeyrium_sensor.dart';

void main() => runApp(const MyApp());

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  MyAppState createState() => MyAppState();
}

class MyAppState extends State<MyApp> {
  String _data = "";

  late StreamSubscription<dynamic> _streamSubscriptions;

  @override
  void initState() {
    _streamSubscriptions = AeyriumSensor.sensorEvents.listen((event) {
      setState(() {
        _data = "Pitch ${event.x} , Roll ${event.y}";
      });
    });
    super.initState();
  }

  @override
  void dispose() {
    _streamSubscriptions.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Padding(
          padding: const EdgeInsets.all(15.0),
          child: Center(
            child: Text('Device : $_data'),
          ),
        ),
      ),
    );
  }
}
