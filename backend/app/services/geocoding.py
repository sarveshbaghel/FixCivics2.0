"""
CivicFix - Reverse Geocoding Service
Google Maps → Nominatim fallback → Mock address
"""
import logging
import httpx
from app.config import settings

logger = logging.getLogger("civicfix.geocoding")


async def reverse_geocode(latitude: float, longitude: float) -> str:
    """
    Reverse geocode coordinates to a human-readable address.
    Falls back through: Google Maps → Nominatim → Mock
    """
    if settings.MOCK_MODE:
        return _mock_address(latitude, longitude)

    # Try Google Maps first
    if settings.GOOGLE_MAPS_API_KEY:
        try:
            address = await _google_maps_geocode(latitude, longitude)
            if address:
                return address
        except Exception as e:
            logger.warning(f"Google Maps geocoding failed: {e}")

    # Fallback to Nominatim (OpenStreetMap)
    try:
        address = await _nominatim_geocode(latitude, longitude)
        if address:
            return address
    except Exception as e:
        logger.warning(f"Nominatim geocoding failed: {e}")

    # Final fallback
    return _mock_address(latitude, longitude)


async def _google_maps_geocode(lat: float, lon: float) -> str | None:
    """Reverse geocode using Google Maps Platform."""
    url = "https://maps.googleapis.com/maps/api/geocode/json"
    params = {
        "latlng": f"{lat},{lon}",
        "key": settings.GOOGLE_MAPS_API_KEY,
    }
    async with httpx.AsyncClient(timeout=10.0) as client:
        resp = await client.get(url, params=params)
        resp.raise_for_status()
        data = resp.json()

    if data.get("status") == "OK" and data.get("results"):
        return data["results"][0].get("formatted_address")
    return None


async def _nominatim_geocode(lat: float, lon: float) -> str | None:
    """Reverse geocode using OpenStreetMap Nominatim (free fallback)."""
    url = "https://nominatim.openstreetmap.org/reverse"
    params = {
        "lat": lat,
        "lon": lon,
        "format": "json",
        "addressdetails": 1,
    }
    headers = {"User-Agent": "CivicFix/1.0"}
    async with httpx.AsyncClient(timeout=10.0) as client:
        resp = await client.get(url, params=params, headers=headers)
        resp.raise_for_status()
        data = resp.json()

    return data.get("display_name")


def _mock_address(lat: float, lon: float) -> str:
    """Generate a mock address for development."""
    address = f"Mock Address near ({lat:.4f}, {lon:.4f}), CivicFix City, 10001"
    logger.info(f"[MOCK] Geocoded: {address}")
    return address
