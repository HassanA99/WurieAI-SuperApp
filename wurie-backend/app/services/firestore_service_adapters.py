from __future__ import annotations

from app.services.firestore_repository import FirestoreRepository
from app.services.market_service import MarketService
from app.services.provider_service import ProviderService
from app.services.booking_service import BookingService
from app.services.ride_service import RideService
from app.services.wallet_service import WalletService


class FirestoreServiceAdapters:
    """Connect the new service-domain adapters to future Firestore collections.

    This adapter is intentionally simple so the business services can evolve
    from local demo data into production Firestore-backed domain services.
    """

    def __init__(self) -> None:
        self.markets = FirestoreRepository("markets")
        self.providers = FirestoreRepository("providers")
        self.bookings = FirestoreRepository("bookings")
        self.rides = FirestoreRepository("rides")
        self.wallets = FirestoreRepository("wallets")

        self.market_service = MarketService()
        self.provider_service = ProviderService()
        self.booking_service = BookingService()
        self.ride_service = RideService()
        self.wallet_service = WalletService()
