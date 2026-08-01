package CFBsimPack;

/**
 * Two resources spent by a roster offer: scholarship slots (cap) and annual NIL purse cash.
 * Scholarships are free in dollars; only NIL hits the recruiting purse.
 */
public final class OfferResourceCost {
    public final int scholarshipSlots;
    public final int annualNilCash;

    public OfferResourceCost(int scholarshipSlots, int annualNilCash) {
        this.scholarshipSlots = Math.max(0, scholarshipSlots);
        this.annualNilCash = Math.max(0, annualNilCash);
    }
}
