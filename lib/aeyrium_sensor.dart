import 'dart:async';

import 'package:flutter/services.dart';

const EventChannel _sensorEventChannel = EventChannel('plugins.aeyrium.com/sensor');

class SensorEvent {
  final x;
  final y;
  final z;
  final w;

  SensorEvent(this.x, this.y, this.z, this.w);

  @override
  String toString() =>
      '[SensorEvent: (x: $x, y: $y, z: $z, w: $w)]';
}

class AeyriumSensor {
  static Stream<SensorEvent> _sensorEvents;

  AeyriumSensor._();

  /// A broadcast stream of events from the device rotation sensor.
  static Stream<SensorEvent> get sensorEvents {
    if (_sensorEvents == null) {
      _sensorEvents = _sensorEventChannel
          .receiveBroadcastStream()
          .map((dynamic event) => _listToSensorEvent(event.cast<double>()));
    }
    return _sensorEvents;
  }

  static SensorEvent _listToSensorEvent(List<double> list) {
    return SensorEvent(list[0], list[1], list[2], list[4]);
  }
}
