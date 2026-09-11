from __future__ import annotations

import firebase_admin
from firebase_admin import firestore

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
        client = None
        if firebase_admin._apps:
            try:
                client = firestore.client()
            except Exception:
                # Firebase may have an app object but no usable local credentials.
                client = None
        self.markets = FirestoreRepository("markets", client)
        self.providers = FirestoreRepository("providers", client)
        self.bookings = FirestoreRepository("bookings", client)
        self.rides = FirestoreRepository("rides", client)
        self.wallets = FirestoreRepository("wallets", client)

        self.market_service = MarketService()
        self.provider_service = ProviderService(datastore=self.providers)
        self.booking_service = BookingService(datastore=self.bookings)
        self.ride_service = RideService()
        self.wallet_service = WalletService(datastore=self.wallets)
