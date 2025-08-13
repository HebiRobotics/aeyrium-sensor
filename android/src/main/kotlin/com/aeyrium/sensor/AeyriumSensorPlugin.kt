package com.aeyrium.sensor

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/**
 * AeyriumSensorPlugin
 */
class AeyriumSensorPlugin : FlutterPlugin, StreamHandler, SensorEventListener {

    private val mVec4Rotation = FloatArray(4)
    private val mMat4Rotation = FloatArray(16)
    private val mMat4RotDisplay = FloatArray(16)
    private val mMat4RotRemappedXZ = FloatArray(16)
    private val mVec4Orientation = DoubleArray(4)
    private val mVec4TempOrientation = FloatArray(4)

    private var mSensorManager: SensorManager? = null
    private var mSensor: Sensor? = null
    private var mLastAccuracy = 0
    private var mEventSink: EventChannel.EventSink? = null

    fun setup(c: Context, m: BinaryMessenger?) {
        val sensorChannel: EventChannel = EventChannel(m, SENSOR_CHANNEL_NAME)
        mSensorManager = c.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        kotlin.checkNotNull(mSensorManager) { "Sensor Manager not found" }
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        sensorChannel.setStreamHandler(this)
    }

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        setup(flutterPluginBinding.getApplicationContext(), flutterPluginBinding.getBinaryMessenger())
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        // TODO: your plugin is no longer attached to a Flutter experience.
    }

    override fun onListen(arguments: Object?, events: EventChannel.EventSink?) {
        Arrays.fill(mVec4Rotation, 0)
        mEventSink = events
        mSensorManager.registerListener(this, mSensor, SENSOR_DELAY_uS)
    }

    override fun onCancel(arguments: Object?) {
        mSensorManager.unregisterListener(this, mSensor)
        mEventSink = null
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (mLastAccuracy != accuracy) {
            mLastAccuracy = accuracy
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (mLastAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE || mEventSink == null) {
            return
        }

        // Low-pass filter the incoming event data to smooth it out.
        lowPassFilter(event.values, mVec4Rotation, 0.3f)

        // Get the rotation matrix from the smoothed event rotation vector.
        SensorManager.getQuaternionFromVector(mVec4TempOrientation, mVec4Rotation)

        // Adjust the matrix to take into account the device orientation
        // Capture the orientation vector from the rotation matrix.
        //SensorManager.getOrientation(mMat4RotRemappedXZ, mVec3Orientation);
        // Android uses wxyz, Event sink expects xyzw, reorder
        mVec4Orientation[3] = mVec4TempOrientation[0].toDouble()
        mVec4Orientation[0] = mVec4TempOrientation[1].toDouble()
        mVec4Orientation[1] = mVec4TempOrientation[2].toDouble()
        mVec4Orientation[2] = mVec4TempOrientation[3].toDouble()

        mEventSink.success(mVec4Orientation)
    }

    companion object {
        private const val SENSOR_CHANNEL_NAME = "plugins.aeyrium.com/sensor"
        private val SENSOR_DELAY_uS: Int = SensorManager.SENSOR_DELAY_UI

        private fun lowPassFilter(input: FloatArray, prev: FloatArray, alpha: Float) {
            if (input == null || prev == null) {
                throw NullPointerException("input and prev float arrays must be non-NULL")
            }
            val length: Int = Math.min(input.size, prev.size)
            for (i in 0..<length) {
                prev[i] = prev[i] + alpha * (input[i] - prev[i])
            }
        }

        /**
         * https://android-developers.blogspot.co.uk/2010/09/one-screen-turn-deserves-another.html
         */
        @SuppressWarnings("SuspiciousNameCombination")
        private fun remapRotationMatrixByDisplay(
            windowManager: WindowManager,
            outMatrix: FloatArray,
            inMatrix: FloatArray
        ) {
            var x: Int = SensorManager.AXIS_X
            var y: Int = SensorManager.AXIS_Y
            when (windowManager.getDefaultDisplay().getRotation()) {
                Surface.ROTATION_0 -> {
                    x = SensorManager.AXIS_X
                    y = SensorManager.AXIS_Y
                }

                Surface.ROTATION_90 -> {
                    x = SensorManager.AXIS_Y
                    y = SensorManager.AXIS_MINUS_X
                }

                Surface.ROTATION_180 -> {
                    x = SensorManager.AXIS_MINUS_X
                    y = SensorManager.AXIS_MINUS_Y
                }

                Surface.ROTATION_270 -> {
                    x = SensorManager.AXIS_MINUS_Y
                    y = SensorManager.AXIS_X
                }
            }
            SensorManager.remapCoordinateSystem(inMatrix, x, y, outMatrix)
        }
  }

}
