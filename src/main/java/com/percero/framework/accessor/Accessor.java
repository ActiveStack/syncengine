package com.percero.framework.accessor;

import javax.persistence.*;

@Entity
public class Accessor extends _Super_Accessor implements IAccessor
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -417768094260185470L;

	public int toInt() {
		int result = 0;
		int one = 1;
		
		Boolean createBoolean = getCanCreate();
		Boolean updateBoolean = getCanUpdate();
		Boolean readBoolean = getCanRead();
		Boolean deleteBoolean = getCanDelete();
		
		if (createBoolean != null && createBoolean)
			result = result | (one << 0);
		if (deleteBoolean != null && deleteBoolean)
			result = result | (one << 1);
		if (readBoolean != null && readBoolean)
			result = result | (one << 2);
		if (updateBoolean != null && updateBoolean)
			result = result | (one << 3);
		
		return result;
	}
	
	@Override
	public String toString() {
		String result = super.toString() + " ";
		if (getCanCreate())
			result += "Create ";
		if (getCanDelete())
			result += "Delete ";
		if (getCanRead())
			result += "Read ";
		if (getCanUpdate())
			result += "Update ";
		
		return result;
	}

}
