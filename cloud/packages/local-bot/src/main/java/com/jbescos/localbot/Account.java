package com.jbescos.localbot;

import java.util.List;

/*
{
  "makerCommission": 15,
  "takerCommission": 15,
  "buyerCommission": 0,
  "sellerCommission": 0,
  "canTrade": true,
  "canWithdraw": true,
  "canDeposit": true,
  "updateTime": 123456789,
  "accountType": "SPOT",
  "balances": [
    {
      "asset": "BTC",
      "free": "4723846.89208129",
      "locked": "0.00000000"
    },
    {
      "asset": "LTC",
      "free": "4763368.68006011",
      "locked": "0.00000000"
    }
  ],
  "permissions": [
    "SPOT"
  ]
}
*/
public class Account {

	private double makerCommission;
	private double takerCommission;
	private double buyerCommission;
	private double sellerCommission;
	private boolean canTrade;
	private boolean canWithdraw;
	private boolean canDeposit;
	private long updateTime;
	private String accountType;
	private List<Balances> balances;
	private List<String> permissions;
	
	public double getMakerCommission() {
		return makerCommission;
	}

	public void setMakerCommission(double makerCommission) {
		this.makerCommission = makerCommission;
	}

	public double getTakerCommission() {
		return takerCommission;
	}

	public void setTakerCommission(double takerCommission) {
		this.takerCommission = takerCommission;
	}

	public double getBuyerCommission() {
		return buyerCommission;
	}

	public void setBuyerCommission(double buyerCommission) {
		this.buyerCommission = buyerCommission;
	}

	public double getSellerCommission() {
		return sellerCommission;
	}

	public void setSellerCommission(double sellerCommission) {
		this.sellerCommission = sellerCommission;
	}

	public boolean isCanTrade() {
		return canTrade;
	}

	public void setCanTrade(boolean canTrade) {
		this.canTrade = canTrade;
	}

	public boolean isCanWithdraw() {
		return canWithdraw;
	}

	public void setCanWithdraw(boolean canWithdraw) {
		this.canWithdraw = canWithdraw;
	}

	public boolean isCanDeposit() {
		return canDeposit;
	}

	public void setCanDeposit(boolean canDeposit) {
		this.canDeposit = canDeposit;
	}

	public long getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(long updateTime) {
		this.updateTime = updateTime;
	}

	public String getAccountType() {
		return accountType;
	}

	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}

	public List<Balances> getBalances() {
		return balances;
	}

	public void setBalances(List<Balances> balances) {
		this.balances = balances;
	}

	public List<String> getPermissions() {
		return permissions;
	}

	public void setPermissions(List<String> permissions) {
		this.permissions = permissions;
	}
	
	@Override
	public String toString() {
		return "Account [makerCommission=" + makerCommission + ", takerCommission=" + takerCommission
				+ ", buyerCommission=" + buyerCommission + ", sellerCommission=" + sellerCommission + ", canTrade="
				+ canTrade + ", canWithdraw=" + canWithdraw + ", canDeposit=" + canDeposit + ", updateTime="
				+ updateTime + ", accountType=" + accountType + ", balances=" + balances + ", permissions="
				+ permissions + "]";
	}

	public static class Balances {
		private String asset;
		private String free;
		private String locked;
		public String getAsset() {
			return asset;
		}
		public void setAsset(String asset) {
			this.asset = asset;
		}
		public String getFree() {
			return free;
		}
		public void setFree(String free) {
			this.free = free;
		}
		public String getLocked() {
			return locked;
		}
		public void setLocked(String locked) {
			this.locked = locked;
		}
		@Override
		public String toString() {
			return "Balances [asset=" + asset + ", free=" + free + ", locked=" + locked + "]";
		}
	}
}
