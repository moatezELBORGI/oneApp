import 'package:flutter/material.dart';
import 'package:mgi/providers/channel_provider.dart';
import 'package:mgi/providers/chat_provider.dart';
import 'package:mgi/providers/notification_provider.dart';
import 'package:provider/provider.dart';
import '../../providers/auth_provider.dart';
import '../../utils/app_theme.dart';
import '../../models/building_selection_model.dart';

class BuildingSwitchScreen extends StatefulWidget {
  const BuildingSwitchScreen({super.key});

  @override
  State<BuildingSwitchScreen> createState() => _BuildingSwitchScreenState();
}

class _BuildingSwitchScreenState extends State<BuildingSwitchScreen> {
  List<BuildingSelection> _buildings = [];
  bool _isLoading = true;
  String? _error;
  String? _currentBuildingId;

  @override
  void initState() {
    super.initState();
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    _currentBuildingId = authProvider.user?.buildingId;
    _loadUserBuildings();
  }

  void _loadUserBuildings() async {
    try {
      setState(() {
        _isLoading = true;
        _error = null;
      });

      final authProvider = Provider.of<AuthProvider>(context, listen: false);
      final response = await authProvider.getUserBuildings();
      final buildings = response
          .map((json) => BuildingSelection.fromJson(json))
          .toList();

      setState(() {
        _buildings = buildings;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString();
        _isLoading = false;
      });
    }
  }

  void _switchBuilding(BuildingSelection building) async {
    if (building.buildingId == _currentBuildingId) {
      return; // Déjà connecté à ce bâtiment
    }

    print('DEBUG: Switching from building $_currentBuildingId to ${building.buildingId}');

    // Nettoyer TOUTES les données AVANT de changer de bâtiment
    _clearAllProviderData();

    final authProvider = Provider.of<AuthProvider>(context, listen: false);

    final success = await authProvider.selectBuilding(building.buildingId);

    if (success && mounted) {
      print('DEBUG: Building switch successful, clearing all data');

      print('DEBUG: All data cleared, navigating to main screen');

      Navigator.of(context).pushNamedAndRemoveUntil('/main', (route) => false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Connecté à ${building.buildingLabel}'),
          backgroundColor: AppTheme.successColor,
        ),
      );
    }
  }

  void _clearAllProviderData() {
    try {
      final chatProvider = Provider.of<ChatProvider>(context, listen: false);
      final channelProvider = Provider.of<ChannelProvider>(context, listen: false);
      final notificationProvider = Provider.of<NotificationProvider>(context, listen: false);
      final voteProvider = Provider.of<VoteProvider>(context, listen: false);

      chatProvider.clearAllData();
      channelProvider.clearAllData();
      notificationProvider.clearAllNotifications();
      voteProvider.clearAllData();

      print('DEBUG: All provider data cleared for building switch');
    } catch (e) {
      print('DEBUG: Error clearing provider data: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      appBar: AppBar(
        title: const Text('Changer d\'immeuble'),
        backgroundColor: Colors.white,
        elevation: 0,
      ),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_error != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.error_outline, size: 64, color: Colors.grey[400]),
            const SizedBox(height: 16),
            Text(
              'Erreur: $_error',
              style: TextStyle(color: Colors.grey[600]),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: _loadUserBuildings,
              child: const Text('Réessayer'),
            ),
          ],
        ),
      );
    }

    if (_buildings.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.apartment_outlined,
              size: 64,
              color: Colors.grey[400],
            ),
            const SizedBox(height: 16),
            Text(
              'Aucun immeuble disponible',
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
      itemCount: _buildings.length,
      itemBuilder: (context, index) {
        final building = _buildings[index];
        final isCurrentBuilding = building.buildingId == _currentBuildingId;

        return _buildBuildingCard(building, isCurrentBuilding);
      },
    );
  }

  Widget _buildBuildingCard(BuildingSelection building, bool isCurrentBuilding) {
    return Card(
      margin: const EdgeInsets.only(bottom: 16),
      elevation: isCurrentBuilding ? 4 : 2,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: isCurrentBuilding
            ? const BorderSide(color: AppTheme.primaryColor, width: 2)
            : BorderSide.none,
      ),
      child: InkWell(
        onTap: isCurrentBuilding ? null : () => _switchBuilding(building),
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    width: 60,
                    height: 60,
                    decoration: BoxDecoration(
                      color: _getRoleColor(building.roleInBuilding).withOpacity(0.1),
                      borderRadius: BorderRadius.circular(15),
                    ),
                    child: building.buildingPicture != null
                        ? ClipRRect(
                      borderRadius: BorderRadius.circular(15),
                      child: Image.network(
                        building.buildingPicture!,
                        fit: BoxFit.cover,
                        errorBuilder: (context, error, stackTrace) {
                          return Icon(
                            Icons.apartment,
                            size: 30,
                            color: _getRoleColor(building.roleInBuilding),
                          );
                        },
                      ),
                    )
                        : Icon(
                      Icons.apartment,
                      size: 30,
                      color: _getRoleColor(building.roleInBuilding),
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Expanded(
                              child: Text(
                                building.buildingLabel,
                                style: const TextStyle(
                                  fontSize: 18,
                                  fontWeight: FontWeight.bold,
                                  color: AppTheme.textPrimary,
                                ),
                              ),
                            ),
                            if (isCurrentBuilding)
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                decoration: BoxDecoration(
                                  color: AppTheme.successColor,
                                  borderRadius: BorderRadius.circular(12),
                                ),
                                child: const Text(
                                  'Actuel',
                                  style: TextStyle(
                                    fontSize: 10,
                                    fontWeight: FontWeight.bold,
                                    color: Colors.white,
                                  ),
                                ),
                              ),
                          ],
                        ),
                        if (building.buildingNumber != null)
                          Text(
                            'N° ${building.buildingNumber}',
                            style: const TextStyle(
                              fontSize: 14,
                              color: AppTheme.textSecondary,
                            ),
                          ),
                      ],
                    ),
                  ),
                  _buildRoleChip(building.roleInBuilding),
                ],
              ),

              const SizedBox(height: 16),

              // Address
              if (building.address != null) ...[
                Row(
                  children: [
                    Icon(
                      Icons.location_on,
                      size: 16,
                      color: Colors.grey[600],
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        '${building.address!.address}, ${building.address!.ville} ${building.address!.codePostal}',
                        style: TextStyle(
                          fontSize: 14,
                          color: Colors.grey[600],
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
              ],

              // Apartment info
              if (building.apartmentId != null) ...[
                Row(
                  children: [
                    Icon(
                      Icons.home,
                      size: 16,
                      color: Colors.grey[600],
                    ),
                    const SizedBox(width: 8),
                    Text(
                      'Appartement ${building.apartmentNumber ?? building.apartmentId}',
                      style: TextStyle(
                        fontSize: 14,
                        color: Colors.grey[600],
                      ),
                    ),
                    if (building.apartmentFloor != null) ...[
                      const SizedBox(width: 8),
                      Text(
                        '• Étage ${building.apartmentFloor}',
                        style: TextStyle(
                          fontSize: 14,
                          color: Colors.grey[600],
                        ),
                      ),
                    ],
                  ],
                ),
              ] else if (building.roleInBuilding == 'BUILDING_ADMIN') ...[
                Row(
                  children: [
                    Icon(
                      Icons.admin_panel_settings,
                      size: 16,
                      color: Colors.grey[600],
                    ),
                    const SizedBox(width: 8),
                    Text(
                      'Administrateur de l\'immeuble',
                      style: TextStyle(
                        fontSize: 14,
                        color: Colors.grey[600],
                      ),
                    ),
                  ],
                ),
              ],

              if (!isCurrentBuilding) ...[
                const SizedBox(height: 16),
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    onPressed: () => _switchBuilding(building),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: _getRoleColor(building.roleInBuilding),
                      padding: const EdgeInsets.symmetric(vertical: 12),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                    child: const Text(
                      'Se connecter',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                        color: Colors.white,
                      ),
                    ),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildRoleChip(String role) {
    Color color = _getRoleColor(role);
    String label = _getRoleLabel(role);

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Text(
        label,
        style: TextStyle(
          fontSize: 12,
          fontWeight: FontWeight.w600,
          color: color,
        ),
      ),
    );
  }

  Color _getRoleColor(String role) {
    switch (role) {
      case 'BUILDING_ADMIN':
        return AppTheme.warningColor;
      case 'GROUP_ADMIN':
        return AppTheme.accentColor;
      case 'SUPER_ADMIN':
        return AppTheme.errorColor;
      default:
        return AppTheme.primaryColor;
    }
  }

  String _getRoleLabel(String role) {
    switch (role) {
      case 'BUILDING_ADMIN':
        return 'Admin';
      case 'GROUP_ADMIN':
        return 'Admin Groupe';
      case 'SUPER_ADMIN':
        return 'Super Admin';
      default:
        return 'Résident';
    }
  }
}