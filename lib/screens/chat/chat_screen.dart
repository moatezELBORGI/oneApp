import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:image_picker/image_picker.dart';
import 'package:file_picker/file_picker.dart';
import 'dart:io';
import '../../providers/chat_provider.dart';
import '../../providers/auth_provider.dart';
import '../../utils/app_theme.dart';
import '../../utils/constants.dart';
import '../../models/channel_model.dart';
import '../../models/message_model.dart';
import '../../widgets/message_bubble.dart';
import '../../widgets/typing_indicator.dart';
import '../../services/audio_service.dart';

class ChatScreen extends StatefulWidget {
  final Channel channel;

  const ChatScreen({super.key, required this.channel});

  @override
  State<ChatScreen> createState() => _ChatScreenState();
}

class _ChatScreenState extends State<ChatScreen> {
  final _messageController = TextEditingController();
  final _scrollController = ScrollController();
  final _focusNode = FocusNode();
  bool _isRecording = false;
  bool _isTyping = false;
 bool _hasText = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _loadMessages();
    });
    _messageController.addListener(_onTypingChanged);
  }

  void _loadMessages() {
    final chatProvider = Provider.of<ChatProvider>(context, listen: false);
    chatProvider.loadChannelMessages(widget.channel.id, refresh: true);
  }

  void _onTypingChanged() {
    final chatProvider = Provider.of<ChatProvider>(context, listen: false);
    final isCurrentlyTyping = _messageController.text.isNotEmpty;
   final hasText = _messageController.text.trim().isNotEmpty;
    
    if (isCurrentlyTyping != _isTyping) {
      _isTyping = isCurrentlyTyping;
      chatProvider.sendTypingIndicator(widget.channel.id, _isTyping);
    }
   
   if (hasText != _hasText) {
     setState(() {
       _hasText = hasText;
     });
   }
  }

  void _sendMessage({String? content, String type = Constants.messageTypeText}) {
    if (content == null || content.trim().isEmpty) return;

    final chatProvider = Provider.of<ChatProvider>(context, listen: false);
    chatProvider.sendMessage(widget.channel.id, content.trim(), type);
    
    _messageController.clear();
   setState(() {
     _hasText = false;
   });
    
    // Scroll to bottom après un petit délai pour laisser le temps au message d'être ajouté
    Future.delayed(const Duration(milliseconds: 100), () {
      _scrollToBottom();
    });
  }

  void _scrollToBottom() {
    if (_scrollController.hasClients) {
      _scrollController.animateTo(
        0,
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeOut,
      );
    }
  }

  void _pickImage() async {
    final picker = ImagePicker();
    final image = await picker.pickImage(source: ImageSource.gallery);
    
    if (image != null) {
      // TODO: Upload image and send message
      _sendMessage(content: image.path, type: Constants.messageTypeImage);
    }
  }

  void _pickFile() async {
    final result = await FilePicker.platform.pickFiles();
    
    if (result != null && result.files.single.path != null) {
      // TODO: Upload file and send message
      _sendMessage(content: result.files.single.path!, type: Constants.messageTypeFile);
    }
  }

  void _startRecording() async {
    setState(() {
      _isRecording = true;
    });
    
    // TODO: Implement audio recording
    await Future.delayed(const Duration(seconds: 1));
    
    setState(() {
      _isRecording = false;
    });
    
    // TODO: Send audio message
    _sendMessage(content: 'Audio message', type: 'AUDIO');
  }

  @override
  void dispose() {
    _messageController.removeListener(_onTypingChanged);
    _messageController.dispose();
    _scrollController.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.grey[50],
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              widget.channel.name,
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w600,
              ),
            ),
            Text(
              '${widget.channel.memberCount} membres',
              style: const TextStyle(
                fontSize: 12,
                color: AppTheme.textSecondary,
              ),
            ),
          ],
        ),
        backgroundColor: Colors.white,
        elevation: 1,
        actions: [
          IconButton(
            onPressed: () {
              // TODO: Show channel info
            },
            icon: const Icon(Icons.info_outline),
          ),
        ],
      ),
      body: Column(
        children: [
          // Messages List
          Expanded(
            child: Consumer<ChatProvider>(
              builder: (context, chatProvider, child) {
                final messages = chatProvider.getChannelMessages(widget.channel.id);
                final typingUsers = chatProvider.getTypingUsers(widget.channel.id);
                
                if (chatProvider.isLoadingMessages(widget.channel.id)) {
                  return const Center(child: CircularProgressIndicator());
                }

                if (messages.isEmpty) {
                  return Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(
                          Icons.chat_bubble_outline,
                          size: 64,
                          color: Colors.grey[400],
                        ),
                        const SizedBox(height: 16),
                        Text(
                          'Aucun message',
                          style: TextStyle(
                            fontSize: 18,
                            color: Colors.grey[600],
                          ),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          'Soyez le premier à envoyer un message !',
                          style: TextStyle(color: Colors.grey[500]),
                        ),
                      ],
                    ),
                  );
                }

                return ListView.builder(
                  controller: _scrollController,
                  reverse: true,
                  padding: const EdgeInsets.all(16),
                  itemCount: messages.length + (typingUsers.isNotEmpty ? 1 : 0),
                  itemBuilder: (context, index) {
                    if (index == 0 && typingUsers.isNotEmpty) {
                      return TypingIndicator(users: typingUsers);
                    }
                    
                    final messageIndex = typingUsers.isNotEmpty ? index - 1 : index;
                    final message = messages[messageIndex];
                    final currentUserId = Provider.of<AuthProvider>(context, listen: false).user?.id;
                    final isMe = message.senderId == currentUserId;
                    
                    // Debug log pour vérifier l'identification
                    if (messageIndex < 3) { // Log seulement les 3 premiers messages pour éviter le spam
                      print('DEBUG: Message from ${message.senderId}, current user: $currentUserId, isMe: $isMe');
                    }
                    
                    return MessageBubble(
                      message: message,
                      isMe: isMe,
                    );
                  },
                );
              },
            ),
          ),
          
          // Message Input
          _buildMessageInput(),
        ],
      ),
    );
  }

  Widget _buildMessageInput() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 10,
            offset: const Offset(0, -2),
          ),
        ],
      ),
      child: SafeArea(
        child: Row(
          children: [
            // Attachment Button
            IconButton(
              onPressed: _showAttachmentOptions,
              icon: const Icon(Icons.attach_file),
              color: AppTheme.primaryColor,
            ),
            
            // Message Input Field
            Expanded(
              child: Container(
                decoration: BoxDecoration(
                  color: Colors.grey[100],
                  borderRadius: BorderRadius.circular(24),
                ),
                child: TextField(
                  controller: _messageController,
                  focusNode: _focusNode,
                  maxLines: null,
                  textCapitalization: TextCapitalization.sentences,
                  decoration: const InputDecoration(
                    hintText: 'Tapez votre message...',
                    border: InputBorder.none,
                    contentPadding: EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 12,
                    ),
                  ),
                  onSubmitted: (text) {
                   if (text.trim().isNotEmpty) {
                     _sendMessage(content: text);
                   }
                  },
                 onChanged: (text) {
                   // Le listener _onTypingChanged se charge déjà de la logique
                 },
                ),
              ),
            ),
            
            const SizedBox(width: 8),
            
            // Send/Record Button
            Consumer<ChatProvider>(
              builder: (context, chatProvider, child) {
               return AnimatedSwitcher(
                 duration: const Duration(milliseconds: 200),
                 child: _hasText
                     ? GestureDetector(
                         key: const ValueKey('send'),
                         onTap: () => _sendMessage(content: _messageController.text),
                         child: Container(
                           width: 48,
                           height: 48,
                           decoration: BoxDecoration(
                             color: AppTheme.primaryColor,
                             borderRadius: BorderRadius.circular(24),
                           ),
                           child: const Icon(
                             Icons.send,
                             color: Colors.white,
                             size: 20,
                           ),
                         ),
                       )
                     : GestureDetector(
                         key: const ValueKey('mic'),
                         onLongPress: _startRecording,
                         child: Container(
                           width: 48,
                           height: 48,
                           decoration: BoxDecoration(
                             color: _isRecording 
                                 ? AppTheme.errorColor 
                                 : Colors.grey[400],
                             borderRadius: BorderRadius.circular(24),
                           ),
                           child: Icon(
                             _isRecording ? Icons.stop : Icons.mic,
                             color: Colors.white,
                             size: 20,
                           ),
                         ),
                       ),
               );
              },
            ),
          ],
        ),
      ),
    );
  }

  void _showAttachmentOptions() {
    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) {
        return Container(
          padding: const EdgeInsets.all(20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 40,
                height: 4,
                decoration: BoxDecoration(
                  color: Colors.grey[300],
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
              const SizedBox(height: 20),
              const Text(
                'Envoyer un fichier',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w600,
                ),
              ),
              const SizedBox(height: 20),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: [
                  _buildAttachmentOption(
                    icon: Icons.photo,
                    label: 'Photo',
                    color: Colors.blue,
                    onTap: () {
                      Navigator.pop(context);
                      _pickImage();
                    },
                  ),
                  _buildAttachmentOption(
                    icon: Icons.camera_alt,
                    label: 'Caméra',
                    color: Colors.green,
                    onTap: () {
                      Navigator.pop(context);
                      _takePhoto();
                    },
                  ),
                  _buildAttachmentOption(
                    icon: Icons.insert_drive_file,
                    label: 'Fichier',
                    color: Colors.orange,
                    onTap: () {
                      Navigator.pop(context);
                      _pickFile();
                    },
                  ),
                ],
              ),
              const SizedBox(height: 20),
            ],
          ),
        );
      },
    );
  }

  Widget _buildAttachmentOption({
    required IconData icon,
    required String label,
    required Color color,
    required VoidCallback onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: Column(
        children: [
          Container(
            width: 60,
            height: 60,
            decoration: BoxDecoration(
              color: color.withOpacity(0.1),
              borderRadius: BorderRadius.circular(30),
            ),
            child: Icon(
              icon,
              color: color,
              size: 30,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            label,
            style: const TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }

  void _takePhoto() async {
    final picker = ImagePicker();
    final image = await picker.pickImage(source: ImageSource.camera);
    
    if (image != null) {
      // TODO: Upload image and send message
      _sendMessage(content: image.path, type: Constants.messageTypeImage);
    }
  }
}