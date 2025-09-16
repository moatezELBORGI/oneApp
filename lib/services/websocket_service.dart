import 'dart:convert';
import 'package:stomp_dart_client/stomp.dart';
import 'package:stomp_dart_client/stomp_config.dart';
import 'package:stomp_dart_client/stomp_frame.dart';
import '../utils/constants.dart';
import '../models/message_model.dart';
import 'storage_service.dart';

class WebSocketService {
  static final WebSocketService _instance = WebSocketService._internal();
  factory WebSocketService() => _instance;
  WebSocketService._internal();

  StompClient? _stompClient;
  bool _isConnected = false;
  
  // Callbacks
  Function(Message)? onMessageReceived;
  Function(String, String, bool)? onTypingReceived;
  Function()? onConnected;
  Function()? onDisconnected;

  bool get isConnected => _isConnected;

  Future<void> connect() async {
    if (_isConnected) return;

    final token = await StorageService.getToken();
    if (token == null) return;

    _stompClient = StompClient(
      config: StompConfig(
        url: Constants.wsUrl,
        onConnect: _onConnect,
        onDisconnect: _onDisconnect,
        onStompError: _onError,
        onWebSocketError: _onWebSocketError,
        stompConnectHeaders: {
          'Authorization': 'Bearer $token',
        },
        webSocketConnectHeaders: {
          'Authorization': 'Bearer $token',
        },
      ),
    );

    _stompClient!.activate();
  }

  void _onConnect(StompFrame frame) {
    print('WebSocket connected');
    _isConnected = true;
    onConnected?.call();
  }

  void _onDisconnect(StompFrame frame) {
    print('WebSocket disconnected');
    _isConnected = false;
    onDisconnected?.call();
  }

  void _onError(StompFrame frame) {
    print('WebSocket STOMP error: ${frame.body}');
  }

  void _onWebSocketError(dynamic error) {
    print('WebSocket error: $error');
  }

  void subscribeToChannel(int channelId) {
    if (!_isConnected || _stompClient == null) return;

    // Subscribe to messages
    _stompClient!.subscribe(
      destination: '/topic/channel/$channelId',
      callback: (StompFrame frame) {
        if (frame.body != null) {
          try {
            final messageData = jsonDecode(frame.body!);
            final message = Message.fromJson(messageData);
            onMessageReceived?.call(message);
          } catch (e) {
            print('Error parsing message: $e');
          }
        }
      },
    );

    // Subscribe to typing indicators
    _stompClient!.subscribe(
      destination: '/topic/channel/$channelId/typing',
      callback: (StompFrame frame) {
        if (frame.body != null) {
          try {
            final typingData = jsonDecode(frame.body!);
            onTypingReceived?.call(
              typingData['userId'],
              typingData['channelId'].toString(),
              typingData['isTyping'],
            );
          } catch (e) {
            print('Error parsing typing indicator: $e');
          }
        }
      },
    );
  }

  void unsubscribeFromChannel(int channelId) {
    if (!_isConnected || _stompClient == null) return;

    _stompClient!.unsubscribe(destination: '/topic/channel/$channelId');
    _stompClient!.unsubscribe(destination: '/topic/channel/$channelId/typing');
  }

  void sendMessage(int channelId, String content, String type, {int? replyToId}) {
    if (!_isConnected || _stompClient == null) return;

    final messageData = {
      'channelId': channelId,
      'content': content,
      'type': type,
      if (replyToId != null) 'replyToId': replyToId,
    };

    _stompClient!.send(
      destination: '/app/message.send',
      body: jsonEncode(messageData),
    );
  }

  void sendTypingIndicator(int channelId, bool isTyping) {
    if (!_isConnected || _stompClient == null) return;

    final typingData = {
      'channelId': channelId,
      'isTyping': isTyping,
    };

    _stompClient!.send(
      destination: '/app/message.typing',
      body: jsonEncode(typingData),
    );
  }

  void disconnect() {
    if (_stompClient != null) {
      _stompClient!.deactivate();
      _stompClient = null;
    }
    _isConnected = false;
  }
}