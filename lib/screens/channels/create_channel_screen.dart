import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/channel_provider.dart';
import '../../providers/auth_provider.dart';
import '../../utils/app_theme.dart';
import '../../widgets/custom_text_field.dart';
import '../../widgets/custom_button.dart';

class CreateChannelScreen extends StatefulWidget {
  const CreateChannelScreen({super.key});

  @override
  State<CreateChannelScreen> createState() => _CreateChannelScreenState();
}

class _CreateChannelScreenState extends State<CreateChannelScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _descriptionController = TextEditingController();
  
  String _selectedType = 'GROUP';
  bool _isPrivate = false;

  @override
  void dispose() {
    _nameController.dispose();
    _descriptionController.dispose();
    super.dispose();
  }

  void _createChannel() async {
    if (_formKey.currentState!.validate()) {
      final channelProvider = Provider.of<ChannelProvider>(context, listen: false);
      
      final channelData = {
        'name': _nameController.text.trim(),
        'description': _descriptionController.text.trim(),
        'type': _selectedType,
        'isPrivate': _isPrivate,
      };

      final channel = await channelProvider.createChannel(channelData);
      
      if (channel != null && mounted) {
        Navigator.of(context).pop();
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Canal créé avec succès !'),
            backgroundColor: AppTheme.successColor,
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: const Text('Créer un canal'),
        backgroundColor: Colors.white,
        elevation: 0,
      ),
      body: Form(
        key: _formKey,
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Channel Name
              CustomTextField(
                controller: _nameController,
                label: 'Nom du canal',
                hint: 'Entrez le nom du canal',
                prefixIcon: Icons.forum,
                validator: (value) {
                  if (value == null || value.isEmpty) {
                    return 'Veuillez saisir un nom';
                  }
                  if (value.length < 3) {
                    return 'Le nom doit contenir au moins 3 caractères';
                  }
                  return null;
                },
              ),
              
              const SizedBox(height: 20),
              
              // Description
              CustomTextField(
                controller: _descriptionController,
                label: 'Description (optionnel)',
                hint: 'Décrivez le canal',
                prefixIcon: Icons.description,
                maxLines: 3,
              ),
              
              const SizedBox(height: 24),
              
              // Channel Type
              const Text(
                'Type de canal',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: AppTheme.textPrimary,
                ),
              ),
              const SizedBox(height: 12),
              
              Container(
                decoration: BoxDecoration(
                  border: Border.all(color: Colors.grey[300]!),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Column(
                  children: [
                    RadioListTile<String>(
                      title: const Text('Groupe'),
                      subtitle: const Text('Canal privé pour un groupe spécifique'),
                      value: 'GROUP',
                      groupValue: _selectedType,
                      onChanged: (value) {
                        setState(() {
                          _selectedType = value!;
                        });
                      },
                    ),
                    const Divider(height: 1),
                    RadioListTile<String>(
                      title: const Text('Public'),
                      subtitle: const Text('Canal ouvert à tous les résidents'),
                      value: 'PUBLIC',
                      groupValue: _selectedType,
                      onChanged: (value) {
                        setState(() {
                          _selectedType = value!;
                        });
                      },
                    ),
                  ],
                ),
              ),
              
              const SizedBox(height: 24),
              
              // Privacy Setting
              Container(
                decoration: BoxDecoration(
                  border: Border.all(color: Colors.grey[300]!),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: SwitchListTile(
                  title: const Text('Canal privé'),
                  subtitle: const Text('Seuls les membres invités peuvent rejoindre'),
                  value: _isPrivate,
                  onChanged: (value) {
                    setState(() {
                      _isPrivate = value;
                    });
                  },
                  activeColor: AppTheme.primaryColor,
                ),
              ),
              
              const SizedBox(height: 40),
              
              // Create Button
              Consumer<ChannelProvider>(
                builder: (context, channelProvider, child) {
                  return CustomButton(
                    text: 'Créer le canal',
                    onPressed: channelProvider.isLoading ? null : _createChannel,
                    isLoading: channelProvider.isLoading,
                    icon: Icons.add,
                  );
                },
              ),
              
              // Error Message
              Consumer<ChannelProvider>(
                builder: (context, channelProvider, child) {
                  if (channelProvider.error != null) {
                    return Container(
                      margin: const EdgeInsets.only(top: 16),
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: AppTheme.errorColor.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(
                        channelProvider.error!,
                        style: const TextStyle(
                          color: AppTheme.errorColor,
                          fontSize: 14,
                        ),
                        textAlign: TextAlign.center,
                      ),
                    );
                  }
                  return const SizedBox.shrink();
                },
              ),
            ],
          ),
        ),
      ),
    );
  }
}