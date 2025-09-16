import 'dart:io';
import 'package:flutter_sound/flutter_sound.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:path_provider/path_provider.dart';

class AudioService {
  static final AudioService _instance = AudioService._internal();
  factory AudioService() => _instance;
  AudioService._internal();

  FlutterSoundRecorder? _recorder;
  FlutterSoundPlayer? _player;
  bool _isRecording = false;
  bool _isPlaying = false;

  bool get isRecording => _isRecording;
  bool get isPlaying => _isPlaying;

  Future<void> initialize() async {
    _recorder = FlutterSoundRecorder();
    _player = FlutterSoundPlayer();

    await _recorder!.openRecorder();
    await _player!.openPlayer();
  }

  Future<bool> requestPermissions() async {
    final status = await Permission.microphone.request();
    return status == PermissionStatus.granted;
  }

  Future<String?> startRecording() async {
    if (!await requestPermissions()) {
      return null;
    }

    try {
      final directory = await getTemporaryDirectory();
      final filePath = '${directory.path}/audio_${DateTime.now().millisecondsSinceEpoch}.aac';

      await _recorder!.startRecorder(
        toFile: filePath,
        codec: Codec.aacADTS,
      );

      _isRecording = true;
      return filePath;
    } catch (e) {
      print('Error starting recording: $e');
      return null;
    }
  }

  Future<void> stopRecording() async {
    try {
      await _recorder!.stopRecorder();
      _isRecording = false;
    } catch (e) {
      print('Error stopping recording: $e');
    }
  }

  Future<void> playAudio(String filePath) async {
    try {
      await _player!.startPlayer(
        fromURI: filePath,
        whenFinished: () {
          _isPlaying = false;
        },
      );
      _isPlaying = true;
    } catch (e) {
      print('Error playing audio: $e');
    }
  }

  Future<void> stopPlaying() async {
    try {
      await _player!.stopPlayer();
      _isPlaying = false;
    } catch (e) {
      print('Error stopping playback: $e');
    }
  }

  Future<void> dispose() async {
    await _recorder?.closeRecorder();
    await _player?.closePlayer();
  }
}