import 'package:flutter/material.dart';
import '../models/message_model.dart';
import '../models/user_model.dart';
import '../services/api_service.dart';
import '../services/websocket_service.dart';
import '../services/storage_service.dart';

class ChatProvider with ChangeNotifier {
  final ApiService _apiService = ApiService();
  final WebSocketService _wsService = WebSocketService();

  final Map<int, List<Message>> _channelMessages = {};
  final Map<int, bool> _isLoadingMessages = {};
  final Map<String, bool> _typingUsers = {};
  
  bool _isLoading = false;
  String? _error;

  bool get isLoading => _isLoading;
  String? get error => _error;

  ChatProvider() {
    _wsService.onMessageReceived = _handleNewMessage;
    _wsService.onTypingReceived = _handleTypingIndicator;
  }

  List<Message> getChannelMessages(int channelId) {
    return _channelMessages[channelId] ?? [];
  }

  bool isLoadingMessages(int channelId) {
    return _isLoadingMessages[channelId] ?? false;
  }

  List<String> getTypingUsers(int channelId) {
    return _typingUsers.entries
        .where((entry) => entry.key.startsWith('$channelId:') && entry.value)
        .map((entry) => entry.key.split(':')[1])
        .toList();
  }

  Future<void> loadChannelMessages(int channelId, {bool refresh = false}) async {
    if (_isLoadingMessages[channelId] == true) return;

    _isLoadingMessages[channelId] = true;
    notifyListeners();

    try {
      final response = await _apiService.getChannelMessages(channelId);
      final messages = (response['content'] as List)
          .map((json) => Message.fromJson(json))
          .toList();

      if (refresh) {
        _channelMessages[channelId] = messages;
      } else {
        _channelMessages[channelId] = [
          ...(_channelMessages[channelId] ?? []),
          ...messages,
        ];
      }

      // Subscribe to WebSocket for this channel
      _wsService.subscribeToChannel(channelId);
      
    } catch (e) {
      _setError(e.toString());
    } finally {
      _isLoadingMessages[channelId] = false;
      notifyListeners();
    }
  }

  Future<void> sendMessage(int channelId, String content, String type, {int? replyToId}) async {
    // Récupérer l'ID de l'utilisateur actuel
    final currentUser = StorageService.getUser();
    final currentUserId = currentUser?.id ?? 'unknown';
    
    try {
      // Créer un message temporaire pour l'affichage immédiat
      final tempMessage = Message(
        id: DateTime.now().millisecondsSinceEpoch, // ID temporaire
        channelId: channelId,
        senderId: currentUserId,
        content: content,
        type: type,
        replyToId: replyToId,
        isEdited: false,
        isDeleted: false,
        createdAt: DateTime.now(),
      );
      
      // Ajouter immédiatement le message à la liste locale
      final channelMessages = _channelMessages[channelId] ?? [];
      channelMessages.insert(0, tempMessage);
      _channelMessages[channelId] = channelMessages;
      notifyListeners();
      
      // Send via WebSocket for real-time delivery
      _wsService.sendMessage(channelId, content, type, replyToId: replyToId);
      
      // Also send via REST API as backup
      await _apiService.sendMessage({
        'channelId': channelId,
        'content': content,
        'type': type,
        if (replyToId != null) 'replyToId': replyToId,
      });
    } catch (e) {
      _setError(e.toString());
    }
  }

  Future<void> editMessage(int messageId, String newContent) async {
    try {
      await _apiService.editMessage(messageId, newContent);
      
      // Update local message
      for (final messages in _channelMessages.values) {
        final messageIndex = messages.indexWhere((m) => m.id == messageId);
        if (messageIndex != -1) {
          final updatedMessage = Message(
            id: messages[messageIndex].id,
            channelId: messages[messageIndex].channelId,
            senderId: messages[messageIndex].senderId,
            content: newContent,
            type: messages[messageIndex].type,
            replyToId: messages[messageIndex].replyToId,
            isEdited: true,
            isDeleted: messages[messageIndex].isDeleted,
            createdAt: messages[messageIndex].createdAt,
            updatedAt: DateTime.now(),
          );
          messages[messageIndex] = updatedMessage;
          break;
        }
      }
      notifyListeners();
    } catch (e) {
      _setError(e.toString());
    }
  }

  Future<void> deleteMessage(int messageId) async {
    try {
      await _apiService.deleteMessage(messageId);
      
      // Update local message
      for (final messages in _channelMessages.values) {
        final messageIndex = messages.indexWhere((m) => m.id == messageId);
        if (messageIndex != -1) {
          final updatedMessage = Message(
            id: messages[messageIndex].id,
            channelId: messages[messageIndex].channelId,
            senderId: messages[messageIndex].senderId,
            content: '[Message supprimé]',
            type: messages[messageIndex].type,
            replyToId: messages[messageIndex].replyToId,
            isEdited: messages[messageIndex].isEdited,
            isDeleted: true,
            createdAt: messages[messageIndex].createdAt,
            updatedAt: DateTime.now(),
          );
          messages[messageIndex] = updatedMessage;
          break;
        }
      }
      notifyListeners();
    } catch (e) {
      _setError(e.toString());
    }
  }

  void sendTypingIndicator(int channelId, bool isTyping) {
    _wsService.sendTypingIndicator(channelId, isTyping);
  }

  void _handleNewMessage(Message message) {
    final currentUser = StorageService.getUser();
    final currentUserId = currentUser?.id ?? 'unknown';
    
    final channelMessages = _channelMessages[message.channelId] ?? [];
    
    // Remplacer le message temporaire s'il existe, sinon ajouter le nouveau
    final tempMessageIndex = channelMessages.indexWhere((m) => 
        m.content == message.content && 
        m.senderId == currentUserId &&
        message.senderId == currentUserId &&
        m.createdAt.difference(message.createdAt).abs().inSeconds < 5
    );
    
    if (tempMessageIndex != -1) {
      // Remplacer le message temporaire par le message réel
      channelMessages[tempMessageIndex] = message;
    } else if (!channelMessages.any((m) => m.id == message.id)) {
      // Ajouter le nouveau message s'il n'existe pas déjà
      channelMessages.insert(0, message);
    }
    
    _channelMessages[message.channelId] = channelMessages;
    notifyListeners();
  }

  void _handleTypingIndicator(String userId, String channelId, bool isTyping) {
    final key = '$channelId:$userId';
    _typingUsers[key] = isTyping;
    
    // Remove typing indicator after 3 seconds
    if (isTyping) {
      Future.delayed(const Duration(seconds: 3), () {
        _typingUsers[key] = false;
        notifyListeners();
      });
    }
    
    notifyListeners();
  }

  void clearChannelMessages(int channelId) {
    _channelMessages.remove(channelId);
    _wsService.unsubscribeFromChannel(channelId);
    notifyListeners();
  }

  void _setError(String error) {
    _error = error;
    notifyListeners();
  }

  void clearError() {
    _error = null;
    notifyListeners();
  }
}