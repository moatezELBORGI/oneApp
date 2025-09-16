class Message {
  final int id;
  final int channelId;
  final String senderId;
  final String content;
  final String type;
  final int? replyToId;
  final bool isEdited;
  final bool isDeleted;
  final DateTime createdAt;
  final DateTime? updatedAt;

  Message({
    required this.id,
    required this.channelId,
    required this.senderId,
    required this.content,
    required this.type,
    this.replyToId,
    required this.isEdited,
    required this.isDeleted,
    required this.createdAt,
    this.updatedAt,
  });

  factory Message.fromJson(Map<String, dynamic> json) {
    return Message(
      id: json['id'],
      channelId: json['channelId'],
      senderId: json['senderId'],
      content: json['content'],
      type: json['type'],
      replyToId: json['replyToId'],
      isEdited: json['isEdited'] ?? false,
      isDeleted: json['isDeleted'] ?? false,
      createdAt: DateTime.parse(json['createdAt']),
      updatedAt: json['updatedAt'] != null ? DateTime.parse(json['updatedAt']) : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'channelId': channelId,
      'senderId': senderId,
      'content': content,
      'type': type,
      'replyToId': replyToId,
      'isEdited': isEdited,
      'isDeleted': isDeleted,
      'createdAt': createdAt.toIso8601String(),
      'updatedAt': updatedAt?.toIso8601String(),
    };
  }
}