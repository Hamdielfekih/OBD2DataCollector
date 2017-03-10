package kr.ac.kaist.kse.ic.obd2datacollector.obd2;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.LoadCommand;
import com.github.pires.obd.commands.engine.MassAirFlowCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.engine.RuntimeCommand;
import com.github.pires.obd.commands.engine.ThrottlePositionCommand;

import java.util.ArrayList;

/**
 * TODO put description
 */
public final class ObdConfig {

    public static ArrayList<ObdCommand> getCommands() {
        ArrayList<ObdCommand> cmds = new ArrayList<>();

        // Control
        //cmds.add(new ModuleVoltageCommand());
        //cmds.add(new EquivalentRatioCommand());
        //cmds.add(new DistanceMILOnCommand());
        //cmds.add(new DtcNumberCommand());
        //cmds.add(new TimingAdvanceCommand());
        //cmds.add(new TroubleCodesCommand());
        //cmds.add(new VinCommand());

        // Engine
        cmds.add(new LoadCommand());
        cmds.add(new RPMCommand());
        cmds.add(new RuntimeCommand());
        cmds.add(new MassAirFlowCommand());
        cmds.add(new ThrottlePositionCommand());

        // Fuel
        //cmds.add(new FindFuelTypeCommand());
        //cmds.add(new ConsumptionRateCommand());
        // cmds.add(new AverageFuelEconomyObdCommand());
        //cmds.add(new FuelEconomyCommand());
        //cmds.add(new FuelLevelCommand());
        // cmds.add(new FuelEconomyMAPObdCommand());
        // cmds.add(new FuelEconomyCommandedMAPObdCommand());
        //cmds.add(new FuelTrimCommand(FuelTrim.LONG_TERM_BANK_1));
        //cmds.add(new FuelTrimCommand(FuelTrim.LONG_TERM_BANK_2));
        //cmds.add(new FuelTrimCommand(FuelTrim.SHORT_TERM_BANK_1));
        //cmds.add(new FuelTrimCommand(FuelTrim.SHORT_TERM_BANK_2));
        //cmds.add(new AirFuelRatioCommand());
        //cmds.add(new WidebandAirFuelRatioCommand());
        //cmds.add(new OilTempCommand());

        // Pressure
        //cmds.add(new BarometricPressureCommand());
        //cmds.add(new FuelPressureCommand());
        //cmds.add(new FuelRailPressureCommand());
        //cmds.add(new IntakeManifoldPressureCommand());

        // Temperature
        //cmds.add(new AirIntakeTemperatureCommand());
        //cmds.add(new AmbientAirTemperatureCommand());
        //cmds.add(new EngineCoolantTemperatureCommand());

        // Misc
        cmds.add(new SpeedCommand());


        return cmds;
    }

}
