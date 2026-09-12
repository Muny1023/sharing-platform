import unittest

from app.main import SEARCH_LIMIT, consume_search_quota, extract_search_keyword, search_history


class SearchBehaviorTest(unittest.TestCase):
    def setUp(self):
        search_history.clear()

    def test_extracts_keyword_from_search_request(self):
        self.assertEqual(extract_search_keyword("帮我找 git 教程"), "git 教程")
        self.assertEqual(extract_search_keyword("有没有爵士音乐帖子"), "爵士音乐")
        self.assertIsNone(extract_search_keyword("你好，今天过得怎么样"))

    def test_search_quota_allows_five_requests_then_blocks(self):
        for _ in range(SEARCH_LIMIT):
            self.assertTrue(consume_search_quota(42))
        self.assertFalse(consume_search_quota(42))
        self.assertTrue(consume_search_quota(43))


if __name__ == "__main__":
    unittest.main()
