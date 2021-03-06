/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.api.block;

/**
 * @author WeathFolD
 *
 */
public interface IWirelessReceiver extends IWirelessUser {
    
    double getRequiredEnergy();
    /**
     * Inject some amount of energy into the machine. ALWAYS positive
     * @return energy not injected
     */
    double injectEnergy(double amt);
    /**
     * Pull some energy out of the machine. ALWAYS positive
     * @param amt The amount
     * @return energy really pulled out.
     */
    double pullEnergy(double amt);
    
    /**
     * @return How much energy this receiver can retrieve each tick.
     */
    double getBandwidth();
    
}
