import unittest

from app.main import ChatRequest, ChatResponse, MAX_TOOL_CALLS, extract_search_keyword


class SearchBehaviorTest(unittest.TestCase):
    def test_extracts_keyword_from_search_request(self):
        self.assertEqual(extract_search_keyword("帮我找 git 教程"), "git 教程")
        self.assertEqual(extract_search_keyword("有没有爵士音乐帖子"), "爵士音乐")
        self.assertIsNone(extract_search_keyword("你好，今天过得怎么样"))

    def test_summary_response_has_optional_summary(self):
        response = ChatResponse(reply="已生成", postIds=[], summary="正文要点")
        self.assertEqual(response.summary, "正文要点")
        self.assertEqual(MAX_TOOL_CALLS, 2)

    def test_feedback_and_draft_request_fields_are_optional(self):
        request = ChatRequest(userId=7, conversationId="c1", message="分析评论", feedbackPostId=9, unreadOnly=True)
        self.assertEqual(request.feedbackPostId, 9)
        self.assertTrue(request.unreadOnly)

    def test_invalid_conversation_id_is_replaced_by_client_side_flow(self):
        # 前端可携带旧值；服务端请求模型仍允许接收，Java 层会自动换发新 UUID。
        request = ChatRequest(userId=7, conversationId="legacy-session", message="你好")
        self.assertEqual(request.conversationId, "legacy-session")

    def test_conversation_id_is_opaque(self):
        request = ChatRequest(userId=7, conversationId="legacy_session_v1", message="你好")
        self.assertEqual(request.conversationId, "legacy_session_v1")

if __name__ == "__main__":
    unittest.main()
