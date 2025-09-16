import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/channel_provider.dart';
import '../../providers/auth_provider.dart';
import '../../utils/app_theme.dart';
import '../../models/channel_model.dart';
import '../chat/chat_screen.dart';
import 'create_channel_screen.dart';

class ChannelsScreen extends StatefulWidget {
  const ChannelsScreen({super.key});

  @override
  State<ChannelsScreen> createState() => _ChannelsScreenState();
}

class _ChannelsScreenState extends State<ChannelsScreen> with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _loadChannels();
    });
  }

  void _loadChannels() {
    final channelProvider = Provider.of<ChannelProvider>(context, listen: false);
    channelProvider.loadChannels(refresh: true);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      appBar: AppBar(
        title: const Text('Canaux'),
        backgroundColor: Colors.white,
        elevation: 0,
        bottom: TabBar(
          controller: _tabController,
          labelColor: AppTheme.primaryColor,
          unselectedLabelColor: AppTheme.textSecondary,
          indicatorColor: AppTheme.primaryColor,
          tabs: const [
            Tab(text: 'Tous'),
            Tab(text: 'Groupes'),
            Tab(text: 'Immeuble'),
          ],
        ),
        actions: [
          IconButton(
            onPressed: () {
              Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (context) => const CreateChannelScreen(),
                ),
              );
            },
            icon: const Icon(Icons.add),
          ),
        ],
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          _buildAllChannels(),
          _buildGroupChannels(),
          _buildBuildingChannels(),
        ],
      ),
    );
  }

  Widget _buildAllChannels() {
    return Consumer<ChannelProvider>(
      builder: (context, channelProvider, child) {
        if (channelProvider.isLoading) {
          return const Center(child: CircularProgressIndicator());
        }

        if (channelProvider.error != null) {
          return Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.error_outline, size: 64, color: Colors.grey[400]),
                const SizedBox(height: 16),
                Text(
                  'Erreur: ${channelProvider.error}',
                  style: TextStyle(color: Colors.grey[600]),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 16),
                ElevatedButton(
                  onPressed: _loadChannels,
                  child: const Text('Réessayer'),
                ),
              ],
            ),
          );
        }

        final channels = channelProvider.channels;
        
        if (channels.isEmpty) {
          return Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.forum_outlined, size: 64, color: Colors.grey[400]),
                const SizedBox(height: 16),
                Text(
                  'Aucun canal disponible',
                  style: TextStyle(
                    fontSize: 18,
                    color: Colors.grey[600],
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  'Créez votre premier canal pour commencer',
                  style: TextStyle(color: Colors.grey[500]),
                ),
              ],
            ),
          );
        }

        return RefreshIndicator(
          onRefresh: () async => _loadChannels(),
          child: ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: channels.length,
            itemBuilder: (context, index) {
              final channel = channels[index];
              return _buildChannelCard(channel);
            },
          ),
        );
      },
    );
  }

  Widget _buildGroupChannels() {
    return Consumer<ChannelProvider>(
      builder: (context, channelProvider, child) {
        final groupChannels = channelProvider.getGroupChannels();
        
        if (groupChannels.isEmpty) {
          return Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.group_outlined, size: 64, color: Colors.grey[400]),
                const SizedBox(height: 16),
                Text(
                  'Aucun groupe',
                  style: TextStyle(
                    fontSize: 18,
                    color: Colors.grey[600],
                  ),
                ),
              ],
            ),
          );
        }

        return ListView.builder(
          padding: const EdgeInsets.all(16),
          itemCount: groupChannels.length,
          itemBuilder: (context, index) {
            return _buildChannelCard(groupChannels[index]);
          },
        );
      },
    );
  }

  Widget _buildBuildingChannels() {
    return Consumer<ChannelProvider>(
      builder: (context, channelProvider, child) {
        final buildingChannels = channelProvider.getBuildingChannels();
        
        if (buildingChannels.isEmpty) {
          return Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.apartment_outlined, size: 64, color: Colors.grey[400]),
                const SizedBox(height: 16),
                Text(
                  'Aucun canal d\'immeuble',
                  style: TextStyle(
                    fontSize: 18,
                    color: Colors.grey[600],
                  ),
                ),
              ],
            ),
          );
        }

        return ListView.builder(
          padding: const EdgeInsets.all(16),
          itemCount: buildingChannels.length,
          itemBuilder: (context, index) {
            return _buildChannelCard(buildingChannels[index]);
          },
        );
      },
    );
  }

  Widget _buildChannelCard(Channel channel) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: _getChannelColor(channel.type).withOpacity(0.1),
          child: Icon(
            _getChannelIcon(channel.type),
            color: _getChannelColor(channel.type),
          ),
        ),
        title: Text(
          channel.name,
          style: const TextStyle(
            fontWeight: FontWeight.w600,
          ),
        ),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (channel.description != null)
              Text(
                channel.description!,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            const SizedBox(height: 4),
            Row(
              children: [
                Icon(
                  Icons.people,
                  size: 14,
                  color: Colors.grey[600],
                ),
                const SizedBox(width: 4),
                Text(
                  '${channel.memberCount} membres',
                  style: TextStyle(
                    fontSize: 12,
                    color: Colors.grey[600],
                  ),
                ),
                if (channel.isPrivate) ...[
                  const SizedBox(width: 8),
                  Icon(
                    Icons.lock,
                    size: 14,
                    color: Colors.grey[600],
                  ),
                ],
              ],
            ),
          ],
        ),
        trailing: channel.lastMessage != null
            ? Text(
                _formatTime(channel.lastMessage!.createdAt),
                style: const TextStyle(
                  fontSize: 12,
                  color: AppTheme.textSecondary,
                ),
              )
            : null,
        onTap: () {
          Navigator.of(context).push(
            MaterialPageRoute(
              builder: (context) => ChatScreen(channel: channel),
            ),
          );
        },
      ),
    );
  }

  IconData _getChannelIcon(String type) {
    switch (type) {
      case 'GROUP':
        return Icons.group;
      case 'BUILDING':
        return Icons.apartment;
      case 'PUBLIC':
        return Icons.public;
      default:
        return Icons.forum;
    }
  }

  Color _getChannelColor(String type) {
    switch (type) {
      case 'GROUP':
        return AppTheme.accentColor;
      case 'BUILDING':
        return AppTheme.warningColor;
      case 'PUBLIC':
        return AppTheme.successColor;
      default:
        return AppTheme.primaryColor;
    }
  }

  String _formatTime(DateTime dateTime) {
    final now = DateTime.now();
    final difference = now.difference(dateTime);

    if (difference.inDays > 0) {
      return '${difference.inDays}j';
    } else if (difference.inHours > 0) {
      return '${difference.inHours}h';
    } else if (difference.inMinutes > 0) {
      return '${difference.inMinutes}m';
    } else {
      return 'maintenant';
    }
  }
}