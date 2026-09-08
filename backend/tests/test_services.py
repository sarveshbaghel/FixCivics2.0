"""
CivicFix Backend Tests - Services
"""
import pytest
from app.services.complaint import generate_complaint_text, generate_tweet_text
from app.services.geocoding import _mock_address
from datetime import datetime, timezone


class TestComplaintGenerator:
    def test_complaint_text_contains_fields(self):
        text = generate_complaint_text(
            issue_type="Pothole",
            description="Large pothole on Main Street",
            address="123 Main St, City",
            latitude=40.7128,
            longitude=-74.0060,
        )
        assert "Pothole" in text
        assert "Large pothole" in text
        assert "123 Main St" in text
        assert "40.7128" in text
        assert "CivicFix" in text

    def test_tweet_text_under_280(self):
        text = generate_tweet_text(
            issue_type="Pothole",
            description="A" * 200,  # Long description
            address="A very long address" * 5,
            latitude=40.7128,
            longitude=-74.0060,
        )
        assert len(text) <= 280

    def test_complaint_has_maps_link(self):
        text = generate_complaint_text(
            issue_type="Garbage",
            description="Test",
            address="Test St",
            latitude=12.34,
            longitude=56.78,
        )
        assert "maps.google.com" in text


class TestGeocoding:
    def test_mock_address(self):
        addr = _mock_address(40.7128, -74.0060)
        assert "40.7128" in addr
        assert "Mock" in addr


class TestInputValidation:
    def test_valid_issue_types(self):
        from app.schemas import IssueType
        assert len(IssueType) == 5
        assert IssueType.POTHOLE.value == "Pothole"
        assert IssueType.GARBAGE.value == "Garbage"

    def test_report_status_values(self):
        from app.schemas import ReportStatus
        assert "pending" in [s.value for s in ReportStatus]
        assert "resolved" in [s.value for s in ReportStatus]
