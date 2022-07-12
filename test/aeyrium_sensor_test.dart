import 'dart:async';
import 'package:aeyrium_sensor/aeyrium_sensor.dart';
import 'package:flutter/services.dart';
import 'package:test/test.dart';

void main() {
  test('${AeyriumSensor.sensorEvents} are streamed', () async {
    const String channelName = 'plugins.aeyrium.com/sensor';
    const List<double> sensorData = <double>[1.0, 2.0];

    const StandardMethodCodec standardMethod = StandardMethodCodec();
    const channel = BasicMessageChannel<ByteData>(channelName, BinaryCodec());

    void emitEvent(ByteData event) {
      channel.send(event);
    }

    bool isCanceled = false;
    channel.setMessageHandler((ByteData? message) async {
      final MethodCall methodCall = standardMethod.decodeMethodCall(message);
      if (methodCall.method == 'listen') {
        emitEvent(standardMethod.encodeSuccessEnvelope(sensorData));
        emitEvent(ByteData(0));
        return standardMethod.encodeSuccessEnvelope(null);
      } else if (methodCall.method == 'cancel') {
        isCanceled = true;
        return standardMethod.encodeSuccessEnvelope(null);
      } else {
        fail('Expected listen or cancel');
      }
    });

    final SensorEvent event = await AeyriumSensor.sensorEvents.first;
    expect(event.x, 1.0);
    expect(event.y, 2.0);

    await Future<Null>.delayed(Duration.zero);
    expect(isCanceled, isTrue);
  });
}
