import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../../providers/auth_provider.dart';
import '../../utils/app_theme.dart';
import '../../widgets/custom_button.dart';

class OtpScreen extends StatefulWidget {
  final String email;
  final bool isLogin;

  const OtpScreen({
    super.key,
    required this.email,
    required this.isLogin,
  });

  @override
  State<OtpScreen> createState() => _OtpScreenState();
}

class _OtpScreenState extends State<OtpScreen> {
  final List<TextEditingController> _controllers = List.generate(6, (_) => TextEditingController());
  final List<FocusNode> _focusNodes = List.generate(6, (_) => FocusNode());
  String _otpCode = '';

  @override
  void dispose() {
    for (var controller in _controllers) {
      controller.dispose();
    }
    for (var focusNode in _focusNodes) {
      focusNode.dispose();
    }
    super.dispose();
  }

  void _handleOtpChange(int index, String value) {
    if (value.isNotEmpty) {
      _controllers[index].text = value;
      if (index < 5) {
        _focusNodes[index + 1].requestFocus();
      }
    } else if (value.isEmpty && index > 0) {
      _focusNodes[index - 1].requestFocus();
    }

    _otpCode = _controllers.map((c) => c.text).join();
    setState(() {});
  }

  void _handleVerifyOtp() async {
    if (_otpCode.length != 6) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Veuillez saisir le code complet'),
          backgroundColor: AppTheme.errorColor,
        ),
      );
      return;
    }

    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    bool success;

    if (widget.isLogin) {
      success = await authProvider.verifyLoginOtp(widget.email, _otpCode);
    } else {
      success = await authProvider.verifyRegistrationOtp(widget.email, _otpCode);
    }

    if (success && mounted) {
      if (widget.isLogin) {
        Navigator.of(context).pushReplacementNamed('/main');
      } else {
        // Registration verified, go to login
        Navigator.of(context).pushReplacementNamed('/login');
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Inscription vérifiée ! Vous pouvez maintenant vous connecter.'),
            backgroundColor: AppTheme.successColor,
          ),
        );
      }
    }
  }

  void _resendOtp() {
    // TODO: Implement resend OTP
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('Code renvoyé !'),
        backgroundColor: AppTheme.successColor,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: const Text('Vérification'),
        backgroundColor: Colors.transparent,
        elevation: 0,
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const SizedBox(height: 40),
              
              // Icon
              Center(
                child: Container(
                  width: 80,
                  height: 80,
                  decoration: BoxDecoration(
                    color: AppTheme.primaryColor.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: const Icon(
                    Icons.security,
                    size: 40,
                    color: AppTheme.primaryColor,
                  ),
                ),
              ),
              
              const SizedBox(height: 30),
              
              // Title and Description
              const Text(
                'Code de vérification',
                style: TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                  color: AppTheme.textPrimary,
                ),
                textAlign: TextAlign.center,
              ),
              
              const SizedBox(height: 12),
              
              Text(
                'Nous avons envoyé un code de vérification à\n${widget.email}',
                style: const TextStyle(
                  fontSize: 16,
                  color: AppTheme.textSecondary,
                ),
                textAlign: TextAlign.center,
              ),
              
              const SizedBox(height: 40),
              
              // OTP Input Fields
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: List.generate(6, (index) {
                  return SizedBox(
                    width: 50,
                    height: 60,
                    child: TextFormField(
                      controller: _controllers[index],
                      focusNode: _focusNodes[index],
                      textAlign: TextAlign.center,
                      keyboardType: TextInputType.number,
                      maxLength: 1,
                      style: const TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                      ),
                      decoration: InputDecoration(
                        counterText: '',
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(16),
                          borderSide: const BorderSide(color: Colors.grey),
                        ),
                        focusedBorder: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(16),
                          borderSide: const BorderSide(color: AppTheme.primaryColor, width: 2),
                        ),
                        filled: true,
                        fillColor: Colors.grey[50],
                        contentPadding: const EdgeInsets.symmetric(vertical: 16),
                      ),
                      inputFormatters: [
                        FilteringTextInputFormatter.digitsOnly,
                      ],
                      onChanged: (value) => _handleOtpChange(index, value),
                    ),
                  );
                }),
              ),
              
              const SizedBox(height: 40),
              
              // Verify Button
              Consumer<AuthProvider>(
                builder: (context, authProvider, child) {
                  return CustomButton(
                    text: 'Vérifier',
                    onPressed: authProvider.isLoading ? null : _handleVerifyOtp,
                    isLoading: authProvider.isLoading,
                  );
                },
              ),
              
              // Error Message
              Consumer<AuthProvider>(
                builder: (context, authProvider, child) {
                  if (authProvider.error != null) {
                    return Container(
                      margin: const EdgeInsets.only(top: 16),
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: AppTheme.errorColor.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(
                        authProvider.error!,
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
              
              const SizedBox(height: 30),
              
              // Resend Code
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Text(
                    'Vous n\'avez pas reçu le code ? ',
                    style: TextStyle(color: AppTheme.textSecondary),
                  ),
                  TextButton(
                    onPressed: _resendOtp,
                    child: const Text('Renvoyer'),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}