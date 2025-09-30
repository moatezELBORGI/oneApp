import 'package:flutter/material.dart';
import '../providers/auth_provider.dart';
import '../providers/chat_provider.dart';
import '../providers/channel_provider.dart';
import '../providers/notification_provider.dart';
import '../providers/vote_provider.dart';
import '../services/websocket_service.dart';

class BuildingContextService {
  static final BuildingContextService _instance = BuildingContextService._internal();
  factory BuildingContextService() => _instance;
  BuildingContextService._internal();

  String? _currentBuildingId;
  
  String? get currentBuildingId => _currentBuildingId;

  void setBuildingContext(String buildingId) {
    if (_currentBuildingId != buildingId) {
      print('DEBUG: Building context changed from $_currentBuildingId to $buildingId');
      _currentBuildingId = buildingId;
    }
  }

  void clearBuildingContext() {
    print('DEBUG: Clearing building context');
    _currentBuildingId = null;
  }

  static void clearAllProvidersData(BuildContext context) {
    try {
      print('DEBUG: Clearing all providers data for building switch');
      
      // Nettoyer WebSocket en premier
      final wsService = WebSocketService();
      wsService.clearAllSubscriptions();
      
      // Nettoyer tous les providers
      final chatProvider = Provider.of<ChatProvider>(context, listen: false);
      final channelProvider = Provider.of<ChannelProvider>(context, listen: false);
      final notificationProvider = Provider.of<NotificationProvider>(context, listen: false);
      final voteProvider = Provider.of<VoteProvider>(context, listen: false);

      chatProvider.clearAllData();
      channelProvider.clearAllData();
      notificationProvider.clearAllNotifications();
      voteProvider.clearAllData();

      print('DEBUG: All providers data cleared successfully');
    } catch (e) {
      print('DEBUG: Error clearing providers data: $e');
    }
  }

  static void loadDataForCurrentBuilding(BuildContext context) {
    try {
      print('DEBUG: Loading data for current building');
      
      final authProvider = Provider.of<AuthProvider>(context, listen: false);
      final channelProvider = Provider.of<ChannelProvider>(context, listen: false);
      
      final currentBuildingId = authProvider.user?.buildingId;
      
      if (currentBuildingId != null) {
        // Mettre à jour le contexte
        BuildingContextService().setBuildingContext(currentBuildingId);
        
        // Charger les données
        channelProvider.loadChannels(refresh: true);
        
        print('DEBUG: Data loading initiated for building: $currentBuildingId');
      } else {
        print('DEBUG: No current building ID found');
      }
    } catch (e) {
      print('DEBUG: Error loading data for current building: $e');
    }
  }

  static void refreshCurrentBuildingData(BuildContext context) {
    print('DEBUG: Refreshing current building data');
    clearAllProvidersData(context);
    
    // Attendre un peu pour que le nettoyage soit effectif
    Future.delayed(const Duration(milliseconds: 100), () {
      loadDataForCurrentBuilding(context);
    });
  }
}