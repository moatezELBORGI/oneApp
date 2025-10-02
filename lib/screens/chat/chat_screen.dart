import 'package:flutter/material.dart';
import 'package:mgi/screens/vote/vote_screen.dart';
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
import 'shared_media_screen.dart';

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
  String? _recordingPath;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _loadMessages();
    });
    _messageController.addListener(_onTypingChanged);
  }

  @override
  void dispose() {
    _messageController.removeListener(_onTypingChanged);
    _messageController.dispose();
    _scrollController.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  // ==================== LIFECYCLE METHODS ====================

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

  // ==================== MESSAGE HANDLING ====================

  void _sendMessage({String? content, String type = Constants.messageTypeText}) {
    if (content == null || content.trim().isEmpty) return;

    final chatProvider = Provider.of<ChatProvider>(context, listen: false);
    chatProvider.sendMessage(widget.channel.id, content.trim(), type);

    _messageController.clear();
    setState(() {
      _hasText = false;
    });

    // Scroll to bottom après un petit délai
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

  // ==================== MEDIA HANDLING ====================

  void _pickImage() async {
    final picker = ImagePicker();
    final image = await picker.pickImage(source: ImageSource.gallery);

    if (image != null) {
      final chatProvider = Provider.of<ChatProvider>(context, listen: false);
      try {
        // Upload the file first to get the file ID
        final result = await chatProvider.sendMessageWithFile(
          widget.channel.id,
          File(image.path),
          Constants.messageTypeImage,
        );
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Erreur lors de l\'envoi de l\'image: $e')),
          );
        }
      }
    }
  }

  void _takePhoto() async {
    final picker = ImagePicker();
    final image = await picker.pickImage(source: ImageSource.camera);

    if (image != null) {
      final chatProvider = Provider.of<ChatProvider>(context, listen: false);
      try {
        await chatProvider.sendMessageWithFile(
          widget.channel.id,
          File(image.path),
          Constants.messageTypeImage,
        );
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Erreur lors de l\'envoi de la photo: $e')),
          );
        }
      }
    }
  }

  void _pickFile() async {
    final result = await FilePicker.platform.pickFiles();

    if (result != null && result.files.single.path != null) {
      final chatProvider = Provider.of<ChatProvider>(context, listen: false);
      await chatProvider.sendMessageWithFile(
        widget.channel.id,
        File(result.files.single.path!),
        Constants.messageTypeFile,
      );
    }
  }

  void _startRecording() async {
    final audioService = AudioService();

    // Vérifier les permissions d'abord
    if (!await audioService.requestPermissions()) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Permission microphone requise pour enregistrer'),
            backgroundColor: AppTheme.errorColor,
          ),
        );
      }
      return;
    }

    setState(() {
      _isRecording = true;
    });

    try {
      _recordingPath = await audioService.startRecording();
      if (_recordingPath == null) {
        throw Exception('Impossible de démarrer l\'enregistrement');
      }

      print('DEBUG: Recording started at path: $_recordingPath');
    } catch (e) {
      print('DEBUG: Error starting recording: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Erreur lors de l\'enregistrement: $e')),
        );
      }
      setState(() {
        _isRecording = false;
        _recordingPath = null;
      });
    }
  }

  void _stopRecording() async {
    if (!_isRecording || _recordingPath == null) return;

    final audioService = AudioService();

    try {
      await audioService.stopRecording();

      setState(() {
        _isRecording = false;
      });

      // Vérifier que le fichier existe
      final file = File(_recordingPath!);
      if (await file.exists()) {
        final chatProvider = Provider.of<ChatProvider>(context, listen: false);
        await chatProvider.sendMessageWithFile(
          widget.channel.id,
          file,
          'AUDIO',
        );
        print('DEBUG: Audio message sent successfully');
      } else {
        throw Exception('Fichier audio non trouvé');
      }
    } catch (e) {
      print('DEBUG: Error stopping recording: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Erreur lors de l\'envoi: $e')),
        );
      }
    } finally {
      setState(() {
        _isRecording = false;
        _recordingPath = null;
      });
    }
  }

  void _cancelRecording() async {
    if (!_isRecording) return;

    final audioService = AudioService();

    try {
      await audioService.cancelRecording();
    } catch (e) {
      print('DEBUG: Error cancelling recording: $e');
    } finally {
      setState(() {
        _isRecording = false;
        _recordingPath = null;
      });
    }
  }

  // ==================== UI BUILDERS ====================

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.grey[50],
      appBar: _buildAppBar(),
      body: Column(
        children: [
          Expanded(child: _buildMessagesList()),
          _buildMessageInput(),
        ],
      ),
    );
  }

  PreferredSizeWidget _buildAppBar() {
    return AppBar(
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
            Navigator.of(context).push(
              MaterialPageRoute(
                builder: (context) => SharedMediaScreen(
                  channelId: widget.channel.id,
                  channelName: widget.channel.name,
                ),
              ),
            );
          },
          icon: const Icon(Icons.photo_library_outlined),
          tooltip: 'Médias partagés',
        ),
        IconButton(
          onPressed: () {
            Navigator.of(context).push(
              MaterialPageRoute(
                builder: (context) => VoteScreen(channel: widget.channel),
              ),
            );
          },
          icon: const Icon(Icons.poll),
          tooltip: 'Votes',
        ),
        IconButton(
          onPressed: () {
            _showChannelInfo();
          },
          icon: const Icon(Icons.info_outline),
          tooltip: 'Informations',
        ),
      ],
    );
  }

  void _showChannelInfo() {
    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) => Container(
        padding: const EdgeInsets.all(20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
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
            Text(
              widget.channel.name,
              style: const TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 8),
            if (widget.channel.description != null)
              Text(
                'Sujet: ${widget.channel.description}',
                style: const TextStyle(
                  fontSize: 16,
                  color: AppTheme.textSecondary,
                ),
              ),
            const SizedBox(height: 16),
            Row(
              children: [
                const Icon(Icons.people, size: 16, color: AppTheme.textSecondary),
                const SizedBox(width: 8),
                Text(
                  '${widget.channel.memberCount} membres',
                  style: const TextStyle(color: AppTheme.textSecondary),
                ),
              ],
            ),
            const SizedBox(height: 20),
          ],
        ),
      ),
    );
  }

  Widget _buildMessagesList() {
    return Consumer<ChatProvider>(
      builder: (context, chatProvider, child) {
        final messages = chatProvider.getChannelMessages(widget.channel.id);
        final typingUsers = chatProvider.getTypingUsers(widget.channel.id);

        if (chatProvider.isLoadingMessages(widget.channel.id)) {
          return const Center(child: CircularProgressIndicator());
        }

        if (messages.isEmpty) {
          return _buildEmptyState();
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
            final currentUser = Provider.of<AuthProvider>(context, listen: false).user;
            final isMe = message.senderId == currentUser?.id || message.senderId == currentUser?.email;

            return MessageBubble(
              message: message,
              isMe: isMe,
            );
          },
        );
      },
    );
  }

  Widget _buildEmptyState() {
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
            _buildAttachmentButton(),
            const SizedBox(width: 8),
            _buildTextInput(),
            const SizedBox(width: 8),
            _buildSendButton(),
          ],
        ),
      ),
    );
  }

  Widget _buildAttachmentButton() {
    return IconButton(
      onPressed: _showAttachmentOptions,
      icon: const Icon(Icons.attach_file),
      color: AppTheme.primaryColor,
    );
  }

  Widget _buildTextInput() {
    return Expanded(
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
        ),
      ),
    );
  }

  Widget _buildSendButton() {
    return AnimatedSwitcher(
      duration: const Duration(milliseconds: 200),
      child: _hasText
          ? _buildSendIconButton()
          : _buildMicrophoneButton(),
    );
  }

  Widget _buildSendIconButton() {
    return GestureDetector(
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
    );
  }

  Widget _buildMicrophoneButton() {
    return _isRecording
        ? _buildRecordingControls()
        : GestureDetector(
      key: const ValueKey('mic'),
      onTap: _startRecording,
      child: Container(
        width: 48,
        height: 48,
        decoration: BoxDecoration(
          color: Colors.grey[400],
          borderRadius: BorderRadius.circular(24),
        ),
        child: Icon(
          Icons.mic,
          color: Colors.white,
          size: 20,
        ),
      ),
    );
  }

  Widget _buildRecordingControls() {
    return Row(
      key: const ValueKey('recording'),
      mainAxisSize: MainAxisSize.min,
      children: [
        // Bouton Annuler
        GestureDetector(
          onTap: _cancelRecording,
          child: Container(
            width: 48,
            height: 48,
            decoration: BoxDecoration(
              color: Colors.grey[400],
              borderRadius: BorderRadius.circular(24),
            ),
            child: const Icon(
              Icons.close,
              color: Colors.white,
              size: 20,
            ),
          ),
        ),

        const SizedBox(width: 8),

        // Indicateur d'enregistrement
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          decoration: BoxDecoration(
            color: AppTheme.errorColor.withOpacity(0.1),
            borderRadius: BorderRadius.circular(20),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 8,
                height: 8,
                decoration: const BoxDecoration(
                  color: AppTheme.errorColor,
                  shape: BoxShape.circle,
                ),
              ),
              const SizedBox(width: 8),
              const Text(
                'Enregistrement...',
                style: TextStyle(
                  color: AppTheme.errorColor,
                  fontSize: 12,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ],
          ),
        ),

        const SizedBox(width: 8),

        // Bouton Envoyer
        GestureDetector(
          onTap: _stopRecording,
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
        ),
      ],
    );
  }

  // ==================== ATTACHMENT OPTIONS ====================

  void _showAttachmentOptions() {
    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) => _buildAttachmentBottomSheet(),
    );
  }

  Widget _buildAttachmentBottomSheet() {
    return Container(
      padding: const EdgeInsets.all(20),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          _buildBottomSheetHandle(),
          const SizedBox(height: 20),
          const Text(
            'Envoyer un fichier',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 20),
          _buildAttachmentOptionsRow(),
          const SizedBox(height: 20),
        ],
      ),
    );
  }

  Widget _buildBottomSheetHandle() {
    return Container(
      width: 40,
      height: 4,
      decoration: BoxDecoration(
        color: Colors.grey[300],
        borderRadius: BorderRadius.circular(2),
      ),
    );
  }

  Widget _buildAttachmentOptionsRow() {
    return Row(
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
}