import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;
import '../utils/constants.dart';
import 'storage_service.dart';

class ApiService {
  static final ApiService _instance = ApiService._internal();
  factory ApiService() => _instance;
  ApiService._internal();

  final String baseUrl = Constants.baseUrl;

  Future<Map<String, String>> _getHeaders({bool requiresAuth = true}) async {
    final headers = {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    };

    if (requiresAuth) {
      final token = await StorageService.getToken();
      if (token != null) {
        headers['Authorization'] = 'Bearer $token';
      }
    }

    return headers;
  }

  // Auth endpoints
  Future<Map<String, dynamic>> login(String email, String password) async {
    final response = await http.post(
      Uri.parse('$baseUrl/auth/login'),
      headers: await _getHeaders(requiresAuth: false),
      body: jsonEncode({
        'email': email,
        'password': password,
      }),
    );

    return _handleResponse(response);
  }

  Future<Map<String, dynamic>> verifyLogin(String email, String otpCode) async {
    final response = await http.post(
      Uri.parse('$baseUrl/auth/verify-login'),
      headers: await _getHeaders(requiresAuth: false),
      body: jsonEncode({
        'email': email,
        'otpCode': otpCode,
      }),
    );

    return _handleResponse(response);
  }

  Future<Map<String, dynamic>> register(Map<String, dynamic> userData) async {
    final response = await http.post(
      Uri.parse('$baseUrl/auth/register'),
      headers: await _getHeaders(requiresAuth: false),
      body: jsonEncode(userData),
    );

    return _handleResponse(response);
  }

  Future<Map<String, dynamic>> verifyRegistration(String email, String otpCode) async {
    final response = await http.post(
      Uri.parse('$baseUrl/auth/verify-registration'),
      headers: await _getHeaders(requiresAuth: false),
      body: jsonEncode({
        'email': email,
        'otpCode': otpCode,
      }),
    );

    return _handleResponse(response);
  }

  // Channel endpoints
  Future<Map<String, dynamic>> getChannels({int page = 0, int size = 20}) async {
    final response = await http.get(
      Uri.parse('$baseUrl/channels?page=$page&size=$size'),
      headers: await _getHeaders(),
    );

    return _handleResponse(response);
  }

  Future<Map<String, dynamic>> getChannel(int channelId) async {
    final response = await http.get(
      Uri.parse('$baseUrl/channels/$channelId'),
      headers: await _getHeaders(),
    );

    return _handleResponse(response);
  }

  Future<Map<String, dynamic>> createChannel(Map<String, dynamic> channelData) async {
    final response = await http.post(
      Uri.parse('$baseUrl/channels'),
      headers: await _getHeaders(),
      body: jsonEncode(channelData),
    );

    return _handleResponse(response);
  }

  Future<Map<String, dynamic>> getOrCreateDirectChannel(String otherUserId) async {
    final response = await http.get(
      Uri.parse('$baseUrl/channels/direct/$otherUserId'),
      headers: await _getHeaders(),
    );

    return _handleResponse(response);
  }

  // Message endpoints
  Future<Map<String, dynamic>> getChannelMessages(int channelId, {int page = 0, int size = 50}) async {
    final response = await http.get(
      Uri.parse('$baseUrl/messages/channel/$channelId?page=$page&size=$size'),
      headers: await _getHeaders(),
    );

    return _handleResponse(response);
  }

  Future<Map<String, dynamic>> sendMessage(Map<String, dynamic> messageData) async {
    final response = await http.post(
      Uri.parse('$baseUrl/messages'),
      headers: await _getHeaders(),
      body: jsonEncode(messageData),
    );

    return _handleResponse(response);
  }

  Future<Map<String, dynamic>> editMessage(int messageId, String content) async {
    final response = await http.put(
      Uri.parse('$baseUrl/messages/$messageId?content=${Uri.encodeComponent(content)}'),
      headers: await _getHeaders(),
    );

    return _handleResponse(response);
  }

  Future<void> deleteMessage(int messageId) async {
    final response = await http.delete(
      Uri.parse('$baseUrl/messages/$messageId'),
      headers: await _getHeaders(),
    );

    _handleResponse(response);
  }

  // Residents endpoints
  Future<Map<String, dynamic>> getBuildingResidents(String buildingId) async {
    final response = await http.get(
      Uri.parse('$baseUrl/channels/building/$buildingId/residents'),
      headers: await _getHeaders(),
    );

    return _handleResponse(response);
  }

  // File upload
  Future<Map<String, dynamic>> uploadFile(File file, String type) async {
    // This would typically upload to a file storage service
    // For now, we'll simulate the upload
    await Future.delayed(const Duration(seconds: 2));
    
    return {
      'success': true,
      'url': 'https://example.com/files/${file.path.split('/').last}',
      'type': type,
    };
  }

  Map<String, dynamic> _handleResponse(http.Response response) {
    final data = jsonDecode(response.body);
    
    if (response.statusCode >= 200 && response.statusCode < 300) {
      return data;
    } else {
      throw ApiException(
        message: data['message'] ?? 'Une erreur est survenue',
        statusCode: response.statusCode,
      );
    }
  }
}

class ApiException implements Exception {
  final String message;
  final int statusCode;

  ApiException({required this.message, required this.statusCode});

  @override
  String toString() => message;
}