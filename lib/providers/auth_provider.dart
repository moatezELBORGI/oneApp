import 'package:flutter/material.dart';
import '../models/user_model.dart';
import '../services/api_service.dart';
import '../services/storage_service.dart';
import '../services/websocket_service.dart';

class AuthProvider with ChangeNotifier {
  final ApiService _apiService = ApiService();
  final WebSocketService _wsService = WebSocketService();

  User? _user;
  bool _isLoading = false;
  String? _error;
  String? _currentUserId;

  User? get user => _user;
  bool get isLoading => _isLoading;
  String? get error => _error;
  bool get isAuthenticated => _user != null;
  String? get currentUserId => _currentUserId;

  AuthProvider() {
    _loadUserFromStorage();
  }

  void _loadUserFromStorage() {
    _user = StorageService.getUser();
    _currentUserId = _user?.id;
    if (_user != null) {
      _connectWebSocket();
    }
    notifyListeners();
  }

  Future<bool> login(String email, String password) async {
    _setLoading(true);
    _clearError();

    try {
      final response = await _apiService.login(email, password);
      
      if (response['otpRequired'] == true) {
        _setLoading(false);
        return true; // OTP required, proceed to OTP screen
      }

      // Direct login success (shouldn't happen with current API)
      await _handleLoginSuccess(response);
      return true;
    } catch (e) {
      _setError(e.toString());
      return false;
    } finally {
      _setLoading(false);
    }
  }

  Future<bool> verifyLoginOtp(String email, String otpCode) async {
    _setLoading(true);
    _clearError();

    try {
      final response = await _apiService.verifyLogin(email, otpCode);
      await _handleLoginSuccess(response);
      return true;
    } catch (e) {
      _setError(e.toString());
      return false;
    } finally {
      _setLoading(false);
    }
  }

  Future<bool> register(Map<String, dynamic> userData) async {
    _setLoading(true);
    _clearError();

    try {
      final response = await _apiService.register(userData);
      _setLoading(false);
      return response['otpRequired'] == true;
    } catch (e) {
      _setError(e.toString());
      return false;
    } finally {
      _setLoading(false);
    }
  }

  Future<bool> verifyRegistrationOtp(String email, String otpCode) async {
    _setLoading(true);
    _clearError();

    try {
      await _apiService.verifyRegistration(email, otpCode);
      _setLoading(false);
      return true;
    } catch (e) {
      _setError(e.toString());
      return false;
    } finally {
      _setLoading(false);
    }
  }

  Future<void> _handleLoginSuccess(Map<String, dynamic> response) async {
    final token = response['token'];
    final refreshToken = response['refreshToken'];
    
    if (token != null) {
      await StorageService.saveToken(token);
    }
    if (refreshToken != null) {
      await StorageService.saveRefreshToken(refreshToken);
    }

    _user = User.fromJson(response);
    _currentUserId = _user!.id;
    print('DEBUG: User logged in with ID: ${_user!.id}'); // Debug log
    await StorageService.saveUser(_user!);
    
    await _connectWebSocket();
    notifyListeners();
  }

  Future<void> _connectWebSocket() async {
    try {
      await _wsService.connect();
    } catch (e) {
      print('Failed to connect WebSocket: $e');
    }
  }

  Future<void> logout() async {
    _wsService.disconnect();
    await StorageService.clearAll();
    _user = null;
    _clearError();
    notifyListeners();
  }

  void _setLoading(bool loading) {
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