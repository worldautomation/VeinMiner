package portablejim.veinminer.asm;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.asm.transformers.DeobfuscationTransformer;
import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import cpw.mods.fml.relauncher.IClassTransformer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.*;
import portablejim.veinminer.VeinMiner;

import java.util.HashMap;
import java.util.Iterator;

public class ItemInWorldManagerTransformer implements IClassTransformer {

    public String targetClassName = "portablejim/veinminer/VeinMiner";
    public String targetClassType = "Lportablejim/veinminer/VeinMiner;";
    public String targetMethodName = "blockMined";
    public String targetMethodType = "(%s%sIIIZ)V";
    public static final HashMap<String, String> srgMappings;
    static {
        srgMappings = new HashMap<String, String>();
        srgMappings.put("uncheckedTryHarvestBlock", "func_73082_a");
        srgMappings.put("tryHarvestBlock", "func_73084_b");
        srgMappings.put("theWorld", "field_73092_a");
        srgMappings.put("thisPlayerMP", "field_73090_b");
    }

    private HashMap<String, String> typemap;

    private boolean obfuscated = true;

    public ItemInWorldManagerTransformer() {
        typemap = new HashMap<String, String>();
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {

        if("net.minecraft.item.ItemInWorldManager".equals(transformedName)) {
            obfuscated = !transformedName.equals(name);
            bytes = transformItemInWorldManager(name, bytes);
        }

        return bytes;
    }

    private String getCorrectName(String name) {
        if(obfuscated && srgMappings.containsKey(name)) {
            return srgMappings.get(name);
        }
        return name;
    }

    public boolean isMethodWithName(AbstractInsnNode instruction, String obfuscatedClassName, String name) {
        if(instruction.getType() == AbstractInsnNode.METHOD_INSN) {
            MethodInsnNode methodNode = (MethodInsnNode)instruction;
            String srgName = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(obfuscatedClassName, methodNode.name, methodNode.desc);
            return srgName.equals(getCorrectName(name));
        }
        return false;
    }

    private void transformUncheckedTryHarvestBlock(MethodNode curMethod, String obfuscatedClassName) {
        LocalVariablesSorter varSorter = new LocalVariablesSorter(curMethod.access, curMethod.desc, curMethod);
        int index = 0;
        while(!isMethodWithName(curMethod.instructions.get(index), obfuscatedClassName, "tryHarvestBlock")) {
            ++index;
        }

        // Add variable to store result
        int newVarIndex = varSorter.newLocal(Type.BOOLEAN_TYPE);
        VarInsnNode newVar = new VarInsnNode(Opcodes.ISTORE, newVarIndex);
        curMethod.instructions.insert(curMethod.instructions.get(index), newVar);
        ++index;

        // Add in function call to call function
        InsnList veinMinerFunctionCall = new InsnList();
        veinMinerFunctionCall.add(new FieldInsnNode(Opcodes.GETSTATIC, targetClassName, "instance", targetClassType));
        veinMinerFunctionCall.add(new VarInsnNode(Opcodes.ALOAD, 0));
        veinMinerFunctionCall.add(new FieldInsnNode(Opcodes.GETFIELD, obfuscatedClassName.replace(".", "/"), getCorrectName("theWorld"), typemap.get(getCorrectName("theWorld"))));
        veinMinerFunctionCall.add(new VarInsnNode(Opcodes.ALOAD, 0));
        veinMinerFunctionCall.add(new FieldInsnNode(Opcodes.GETFIELD, obfuscatedClassName.replace(".", "/"), getCorrectName("thisPlayerMP"), typemap.get(getCorrectName("thisPlayerMP"))));
        veinMinerFunctionCall.add(new VarInsnNode(Opcodes.ILOAD, 1));
        veinMinerFunctionCall.add(new VarInsnNode(Opcodes.ILOAD, 2));
        veinMinerFunctionCall.add(new VarInsnNode(Opcodes.ILOAD, 3));
        veinMinerFunctionCall.add(new VarInsnNode(Opcodes.ILOAD, newVarIndex));

        String worldType = typemap.get(getCorrectName("theWorld"));
        String playerType = typemap.get(getCorrectName("thisPlayerMP"));
        veinMinerFunctionCall.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, targetClassName, targetMethodName, String.format(targetMethodType, worldType, playerType)));
        curMethod.instructions.insert(curMethod.instructions.get(index), veinMinerFunctionCall);
        ++index;

        // Get rid of un-needed POP.
        while (curMethod.instructions.get(index).getOpcode() != Opcodes.POP) {
            ++index;
        }
        curMethod.instructions.remove(curMethod.instructions.get(index));
    }

    public byte[] transformItemInWorldManager(String obfuscatedClassName, byte[] bytes) {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(classNode, 0);

        // Setup type map
        for(FieldNode variable : classNode.fields) {
            String srgVariableName = FMLDeobfuscatingRemapper.INSTANCE.mapFieldName(obfuscatedClassName, variable.name, variable.desc);
            FMLLog.getLogger().info(String.format("FIELD: %s | %s", srgVariableName, getCorrectName("theWorld")));
            if(getCorrectName("theWorld").equals(srgVariableName) ||
                    getCorrectName("thisPlayerMP").equals(srgVariableName)) {
                typemap.put(srgVariableName, variable.desc);
            }
        }

        Iterator<MethodNode> methods = classNode.methods.iterator();
        while(methods.hasNext()) {
            MethodNode curMethod = methods.next();
            String srgFunctionName = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(obfuscatedClassName, curMethod.name, curMethod.desc);

            if(getCorrectName("uncheckedTryHarvestBlock").equals(srgFunctionName)) {
                transformUncheckedTryHarvestBlock(curMethod, obfuscatedClassName);
            }
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }
}