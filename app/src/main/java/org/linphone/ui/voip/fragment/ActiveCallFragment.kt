/*
 * Copyright (c) 2010-2023 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.linphone.ui.voip.fragment

import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import org.linphone.R
import org.linphone.databinding.VoipActiveCallFragmentBinding
import org.linphone.ui.main.fragment.GenericFragment
import org.linphone.ui.voip.model.ZrtpSasConfirmationDialogModel
import org.linphone.ui.voip.viewmodel.CurrentCallViewModel
import org.linphone.utils.DialogUtils
import org.linphone.utils.slideInToastFromTop

class ActiveCallFragment : GenericFragment() {
    private lateinit var binding: VoipActiveCallFragmentBinding

    private lateinit var callViewModel: CurrentCallViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = VoipActiveCallFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        callViewModel = requireActivity().run {
            ViewModelProvider(this)[CurrentCallViewModel::class.java]
        }

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = callViewModel

        callViewModel.toggleExtraActionMenuVisibilityEvent.observe(viewLifecycleOwner) {
            /*it.consume { opened ->
                val visibility = if (opened) View.VISIBLE else View.GONE
                binding.extraActions.slideInExtraActionsMenu(binding.root as ViewGroup, visibility)
            }*/
        }

        callViewModel.isRemoteDeviceTrusted.observe(viewLifecycleOwner) { trusted ->
            if (trusted) {
                binding.blueToast.message.text = "This call can be trusted"
                binding.blueToast.icon.setImageResource(R.drawable.trusted)
                binding.blueToast.root.slideInToastFromTop(binding.root as ViewGroup, true)
            } else if (binding.blueToast.root.visibility == View.VISIBLE) {
                binding.blueToast.root.slideInToastFromTop(binding.root as ViewGroup, false)
            }
        }

        callViewModel.showZrtpSasDialogEvent.observe(viewLifecycleOwner) {
            it.consume { pair ->
                val model = ZrtpSasConfirmationDialogModel(pair.first, pair.second)
                val dialog = DialogUtils.getZrtpSasConfirmationDialog(requireActivity(), model)

                model.dismissEvent.observe(viewLifecycleOwner) { event ->
                    event.consume {
                        dialog.dismiss()
                    }
                }

                model.trustVerified.observe(viewLifecycleOwner) { event ->
                    event.consume { verified ->
                        callViewModel.updateZrtpSas(verified)
                        dialog.dismiss()
                    }
                }

                dialog.show()
            }
        }

        callViewModel.callDuration.observe(viewLifecycleOwner) { duration ->
            binding.chronometer.base = SystemClock.elapsedRealtime() - (1000 * duration)
            binding.chronometer.start()
        }
    }
}
