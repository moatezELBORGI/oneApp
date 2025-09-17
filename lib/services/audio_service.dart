import 'dart:io';
import 'package:record/record.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:path_provider/path_provider.dart';

class AudioService {
  static final AudioService _instance = AudioService._internal();
  factory AudioService() => _instance;
  AudioService._internal();

  final AudioRecorder _recorder = AudioRecorder();
  bool _isRecording = false;
  String? _currentRecordingPath;

  bool get isRecording => _isRecording;

  Future<bool> requestPermissions() async {
    try {
      final status = await Permission.microphone.request();
      print('DEBUG: Microphone permission status: $status');
      return status == PermissionStatus.granted;
    } catch (e) {
      print('DEBUG: Error requesting microphone permission: $e');
      return false;
    }
  }

  Future<String?> startRecording() async {
    print('DEBUG: Starting audio recording...');
    
    if (!await requestPermissions()) {
      print('DEBUG: Microphone permission denied');
      return null;
    }

    try {
      // Vérifier si le microphone est disponible
      if (await _recorder.hasPermission()) {
        final directory = await getTemporaryDirectory();
        final filePath = '${directory.path}/audio_${DateTime.now().millisecondsSinceEpoch}.m4a';
        
        print('DEBUG: Recording to path: $filePath');

        await _recorder.start(
          const RecordConfig(
            encoder: AudioEncoder.aacLc,
            bitRate: 128000,
            sampleRate: 44100,
          ),
          path: filePath,
        );

        _isRecording = true;
        _currentRecordingPath = filePath;
        print('DEBUG: Recording started successfully');
        return filePath;
      } else {
        print('DEBUG: No microphone permission');
        return null;
      }
    } catch (e) {
      print('Error starting recording: $e');
      _isRecording = false;
      _currentRecordingPath = null;
      return null;
    }
  }

  Future<void> stopRecording() async {
    print('DEBUG: Stopping audio recording...');
    
    try {
      final path = await _recorder.stop();
      _isRecording = false;
      _currentRecordingPath = null;
      print('DEBUG: Recording stopped. Final path: $path');
    } catch (e) {
      print('Error stopping recording: $e');
      _isRecording = false;
      _currentRecordingPath = null;
    }
  }

  Future<void> cancelRecording() async {
    print('DEBUG: Cancelling audio recording...');
    
    try {
      await _recorder.stop();
      _isRecording = false;
      
      // Supprimer le fichier si il existe
      if (_currentRecordingPath != null) {
        final file = File(_currentRecordingPath!);
        if (await file.exists()) {
          await file.delete();
          print('DEBUG: Recording file deleted');
        }
      }
      _currentRecordingPath = null;
    } catch (e) {
      print('Error cancelling recording: $e');
      _isRecording = false;
      _currentRecordingPath = null;
    }
  }

  Future<bool> isRecordingAvailable() async {
    try {
      return await _recorder.hasPermission();
    } catch (e) {
      print('Error checking recording availability: $e');
      return false;
    }
  }

  String? get currentRecordingPath => _currentRecordingPath;

  Future<void> dispose() async {
    try {
      if (_isRecording) {
        await stopRecording();
      }
      await _recorder.dispose();
    } catch (e) {
      print('Error disposing audio service: $e');
    }
  }
}