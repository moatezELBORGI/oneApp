import 'package:flutter/material.dart';
import '../models/channel_model.dart';
import '../models/user_model.dart';
import '../models/message_model.dart';
import '../services/api_service.dart';

class ChannelProvider with ChangeNotifier {
  final ApiService _apiService = ApiService();

  List<Channel> _channels = [];
  List<User> _buildingResidents = [];
  bool _isLoading = false;
  String? _error;

  List<Channel> get channels => _channels;
  List<User> get buildingResidents => _buildingResidents;
  bool get isLoading => _isLoading;
  String? get error => _error;

  Future<void> loadChannels({bool refresh = false}) async {
    if (_isLoading && !refresh) return;

    _setLoading(true);
    _clearError();

    try {
      final response = await _apiService.getChannels();
      _channels = (response['content'] as List)
          .map((json) => Channel.fromJson(json))
          .toList();

      // Sort channels by last activity
      _channels.sort((a, b) {
        final aTime = a.lastMessage?.createdAt ?? a.createdAt;
        final bTime = b.lastMessage?.createdAt ?? b.createdAt;
        return bTime.compareTo(aTime);
      });

    } catch (e) {
      _setError(e.toString());
    } finally {
      _setLoading(false);
    }
  }

  Future<Channel?> getOrCreateDirectChannel(String otherUserId) async {
    _setLoading(true);
    _clearError();

    try {
      final response = await _apiService.getOrCreateDirectChannel(otherUserId);
      final channel = Channel.fromJson(response);

      // Add to channels list if not exists
      if (!_channels.any((c) => c.id == channel.id)) {
        _channels.insert(0, channel);
        notifyListeners();
      }

      return channel;
    } catch (e) {
      _setError(e.toString());
      return null;
    } finally {
      _setLoading(false);
    }
  }

  Future<Channel?> createChannel(Map<String, dynamic> channelData) async {
    _setLoading(true);
    _clearError();

    try {
      final response = await _apiService.createChannel(channelData);
      final channel = Channel.fromJson(response);

      _channels.insert(0, channel);
      notifyListeners();

      return channel;
    } catch (e) {
      _setError(e.toString());
      return null;
    } finally {
      _setLoading(false);
    }
  }

  Future<void> loadBuildingResidents(String buildingId) async {
    print('DEBUG: Loading residents for current building context');
    _setLoading(true);
    _clearError();

    try {
      final response = await _apiService.getBuildingResidents("current");
      print('DEBUG: API response: $response');

      // Forcer à extraire la liste même si ApiService renvoie Map
      final residentsList = (response as dynamic) as List<dynamic>;

      _buildingResidents = residentsList
          .map((json) => User.fromJson(json))
          .toList();

      print('DEBUG: Parsed ${_buildingResidents.length} residents for current building');
      for (var resident in _buildingResidents) {
        print('DEBUG: Resident: ${resident.fullName} (Building: ${resident.buildingId})');
      }
    } catch (e) {
      print('DEBUG: Error loading residents: $e');
      _setError(e.toString());
    } finally {
      _setLoading(false);
    }
  }


  Channel? getChannelById(int channelId) {
    try {
      return _channels.firstWhere((channel) => channel.id == channelId);
    } catch (e) {
      return null;
    }
  }

  List<Channel> getDirectChannels() {
    return _channels.where((c) => c.type == 'ONE_TO_ONE').toList();
  }

  List<Channel> getGroupChannels() {
    return _channels.where((c) => c.type == 'GROUP').toList();
  }

  List<Channel> getBuildingChannels() {
    return _channels.where((c) => c.type == 'BUILDING').toList();
  }

  void updateChannelLastMessage(int channelId, Message lastMessage) {
    final channelIndex = _channels.indexWhere((c) => c.id == channelId);
    if (channelIndex != -1) {
      final updatedChannel = Channel(
        id: _channels[channelIndex].id,
        name: _channels[channelIndex].name,
        description: _channels[channelIndex].description,
        type: _channels[channelIndex].type,
        buildingId: _channels[channelIndex].buildingId,
        buildingGroupId: _channels[channelIndex].buildingGroupId,
        createdBy: _channels[channelIndex].createdBy,
        isActive: _channels[channelIndex].isActive,
        isPrivate: _channels[channelIndex].isPrivate,
        createdAt: _channels[channelIndex].createdAt,
        updatedAt: _channels[channelIndex].updatedAt,
        memberCount: _channels[channelIndex].memberCount,
        lastMessage: lastMessage,
      );

      _channels[channelIndex] = updatedChannel;

      // Move to top of list
      _channels.removeAt(channelIndex);
      _channels.insert(0, updatedChannel);

      notifyListeners();
    }
  }

  void clearAllData() {
    _channels.clear();
    _buildingResidents.clear();
    _isLoading = false;
    _error = null;
    notifyListeners();
  }

  void _setLoading(bool loading) {

    void clearBuildingResidents() {
      _buildingResidents.clear();
      notifyListeners();
    }
    _isLoading = loading;
    notifyListeners();
  }

  void _setError(String error) {
    _error = error;
    notifyListeners();
  }

  void _clearError() {
    _error = null;
    notifyListeners();
  }
}